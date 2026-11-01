package com.campusnest.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.campusnest.commute.CommuteEstimate;
import com.campusnest.commute.TravelMode;
import com.campusnest.listing.ListingStatus;
import com.campusnest.listing.ListingTypes.GenderPolicy;
import com.campusnest.listing.ListingTypes.RoomType;
import com.campusnest.recommendation.SuitabilityModel.Component;
import com.campusnest.recommendation.SuitabilityModel.Exclusion;
import com.campusnest.recommendation.SuitabilityModel.ListingFacts;
import com.campusnest.recommendation.SuitabilityModel.Preferences;
import com.campusnest.recommendation.SuitabilityModel.Result;
import com.campusnest.recommendation.SuitabilityModel.RoomFacts;
import com.campusnest.student.Gender;

/**
 * FR-1 test cases for the suitability score. Pure unit tests: hand-made inputs, exact expected numbers.
 * Each expected value can be re-calculated by hand from docs/ALGORITHMS.md.
 */
class SuitabilityScorerTest {

    static final Instant NOW = Instant.parse("2026-11-01T12:00:00Z");
    static final Map<String, String> NAMES = Map.of("WIFI", "Wi-Fi", "MEALS", "Meals provided", "AC", "Air conditioning",
            "LAUNDRY", "Laundry");

    /** Budget ₹10,000, 40-minute limit, wants a double (then single), must have Wi-Fi + meals, nice: AC. */
    static Preferences prefs() {
        return new Preferences(10_000, 40, List.of(RoomType.DOUBLE, RoomType.SINGLE),
                new LinkedHashSet<>(List.of("WIFI", "MEALS")), new LinkedHashSet<>(List.of("AC")), Gender.FEMALE);
    }

    static CommuteEstimate commute(int minutes) {
        return new CommuteEstimate(TravelMode.AUTO_RICKSHAW, 3.0, minutes, true, "test");
    }

    /** A strong listing: ₹9,000 double, no extra charges, 15 min away, all amenities yes, verified, fresh. */
    static ListingFacts goodListing() {
        return new ListingFacts(ListingStatus.PUBLISHED_AVAILABLE, GenderPolicy.ANY,
                List.of(new RoomFacts(1L, RoomType.DOUBLE, 9_000, 2)), 0,
                Map.of("WIFI", true, "MEALS", true, "AC", true), NAMES, true, NOW.minus(Duration.ofDays(3)),
                0, 0, 0, commute(15));
    }

    static ListingFacts with(ListingFacts l, java.util.function.UnaryOperator<ListingFacts> change) {
        return change.apply(l);
    }

    static Component part(Result r, String key) {
        return r.components().stream().filter(c -> c.key().equals(key)).findFirst().orElseThrow();
    }

    // ---------------- Required case 1: excellent budget match ----------------

    @Test
    void excellentBudgetMatchScoresFullBudgetPoints() {
        Result r = SuitabilityScorer.score(prefs(), goodListing(), NOW);
        assertThat(r.eligible()).isTrue();
        assertThat(part(r, "budget").score()).isEqualTo(100);
        assertThat(part(r, "budget").explanation()).contains("₹9,000").contains("within your ₹10,000 budget");
    }

    // ---------------- Required case 2: poor budget match ----------------

    @Nested
    class PoorBudget {
        ListingFacts withRent(int rent, int charges) {
            ListingFacts g = goodListing();
            return new ListingFacts(g.status(), g.genderPolicy(), List.of(new RoomFacts(1L, RoomType.DOUBLE, rent, 2)),
                    charges, g.amenityAnswers(), NAMES, g.ownerVerified(), g.lastConfirmedAt(), 0, 0, 0, g.commute());
        }

        @Test
        void fifteenPercentOverBudgetLosesHalfTheBudgetPoints() {
            Result r = SuitabilityScorer.score(prefs(), withRent(11_500, 0), NOW);
            assertThat(part(r, "budget").score()).isEqualTo(50); // 100 × (1 − 0.15/0.30)
            assertThat(part(r, "budget").explanation()).contains("₹1,500 (15%) over");
        }

        @Test
        void mandatoryChargesCountTowardsTheBudget() {
            Result r = SuitabilityScorer.score(prefs(), withRent(9_500, 800), NOW); // ₹10,300 = 3% over
            assertThat(r.monthlyCost()).isEqualTo(10_300);
            assertThat(part(r, "budget").score()).isEqualTo(90);
            assertThat(part(r, "budget").explanation()).contains("₹9,500 rent + ₹800 mandatory charges = ₹10,300");
        }

        @Test
        void exactlyThirtyPercentOverIsShownWithZeroBudgetPoints() {
            Result r = SuitabilityScorer.score(prefs(), withRent(13_000, 0), NOW);
            assertThat(r.eligible()).isTrue();
            assertThat(part(r, "budget").score()).isZero();
        }

        @Test
        void moreThanThirtyPercentOverIsExcluded() {
            Result r = SuitabilityScorer.score(prefs(), withRent(13_001, 0), NOW);
            assertThat(r.eligible()).isFalse();
            assertThat(r.exclusions()).containsExactly(Exclusion.OVER_BUDGET);
        }
    }

    // ---------------- Required case 3: excellent commute match (and the curve) ----------------

    @Test
    void commuteWithinHalfTheLimitIsExcellent() {
        Result r = SuitabilityScorer.score(prefs(), goodListing(), NOW); // 15 min, limit 40
        assertThat(part(r, "commute").score()).isEqualTo(100);
        assertThat(part(r, "commute").explanation()).contains("About 15 min").contains("within your 40-minute limit");
    }

    @Test
    void commuteCurveBoundaries() {
        ListingFacts g = goodListing();
        int[][] cases = {{20, 100}, {30, 85}, {40, 70}, {50, 35}, {60, 0}, {90, 0}}; // minutes -> score, limit 40
        for (int[] c : cases) {
            ListingFacts l = new ListingFacts(g.status(), g.genderPolicy(), g.rooms(), 0, g.amenityAnswers(), NAMES,
                    true, g.lastConfirmedAt(), 0, 0, 0, commute(c[0]));
            assertThat(part(SuitabilityScorer.score(prefs(), l, NOW), "commute").score())
                    .as("%d minutes", c[0]).isEqualTo(c[1]);
        }
    }

    // ---------------- Required case 4: room-type mismatch ----------------

    @Test
    void roomTypeMismatchScoresZeroAndSecondChoiceSeventy() {
        ListingFacts g = goodListing();
        ListingFacts triple = new ListingFacts(g.status(), g.genderPolicy(), List.of(new RoomFacts(1L, RoomType.TRIPLE, 9_000, 2)),
                0, g.amenityAnswers(), NAMES, true, g.lastConfirmedAt(), 0, 0, 0, g.commute());
        ListingFacts single = new ListingFacts(g.status(), g.genderPolicy(), List.of(new RoomFacts(1L, RoomType.SINGLE, 9_000, 2)),
                0, g.amenityAnswers(), NAMES, true, g.lastConfirmedAt(), 0, 0, 0, g.commute());

        assertThat(part(SuitabilityScorer.score(prefs(), triple, NOW), "roomType").score()).isZero();
        assertThat(part(SuitabilityScorer.score(prefs(), triple, NOW), "roomType").explanation()).contains("not one of your preferred");
        assertThat(part(SuitabilityScorer.score(prefs(), single, NOW), "roomType").score()).isEqualTo(70);
    }

    @Test
    void bestRoomIsChosenWhenAListingHasSeveral() {
        ListingFacts g = goodListing();
        ListingFacts twoRooms = new ListingFacts(g.status(), g.genderPolicy(), List.of(
                new RoomFacts(1L, RoomType.TRIPLE, 7_000, 3),   // cheaper but not wanted
                new RoomFacts(2L, RoomType.DOUBLE, 9_500, 1)),  // 1st choice, within budget
                0, g.amenityAnswers(), NAMES, true, g.lastConfirmedAt(), 0, 0, 0, g.commute());
        Result r = SuitabilityScorer.score(prefs(), twoRooms, NOW);
        assertThat(r.bestRoomId()).isEqualTo(2L);
        assertThat(r.bestRoomType()).isEqualTo(RoomType.DOUBLE);
    }

    // ---------------- Required case 5: missing amenities information ----------------

    @Test
    void unknownAmenitiesGetHalfCreditAndLowerDataCompleteness() {
        ListingFacts g = goodListing();
        ListingFacts unknown = new ListingFacts(g.status(), g.genderPolicy(), g.rooms(), 0,
                Map.of("WIFI", true), NAMES, true, g.lastConfirmedAt(), 0, 0, 0, g.commute()); // meals + AC not stated
        Result r = SuitabilityScorer.score(prefs(), unknown, NOW);
        // must: Wi-Fi yes (2), Meals unknown (1 of 2); nice: AC unknown (0.5 of 1) -> 3.5 / 5 = 70
        assertThat(part(r, "amenities").score()).isEqualTo(70);
        assertThat(part(r, "amenities").explanation()).contains("Not specified by owner").contains("Meals provided");
        // commute approximate (0.5) + 1 of 3 amenities known = 1.5 / 4 = 38%
        assertThat(r.dataCompleteness()).isEqualTo(38);
    }

    @Test
    void amenitiesConfirmedMissingScoreZeroForThatItem() {
        ListingFacts g = goodListing();
        ListingFacts noMeals = new ListingFacts(g.status(), g.genderPolicy(), g.rooms(), 0,
                Map.of("WIFI", true, "MEALS", false, "AC", false), NAMES, true, g.lastConfirmedAt(), 0, 0, 0, g.commute());
        Result r = SuitabilityScorer.score(prefs(), noMeals, NOW);
        assertThat(part(r, "amenities").score()).isEqualTo(40); // 2 / 5
        assertThat(part(r, "amenities").explanation()).contains("Not available: Meals provided, Air conditioning");
    }

    // ---------------- Required case 6: unverified listing owner ----------------

    @Test
    void unverifiedOwnerLosesTheVerificationTrustPoints() {
        ListingFacts g = goodListing();
        ListingFacts unverified = new ListingFacts(g.status(), g.genderPolicy(), g.rooms(), 0, g.amenityAnswers(), NAMES,
                false, g.lastConfirmedAt(), 0, 0, 0, g.commute());
        int verifiedTrust = part(SuitabilityScorer.score(prefs(), g, NOW), "trust").score();
        int unverifiedTrust = part(SuitabilityScorer.score(prefs(), unverified, NOW), "trust").score();
        assertThat(verifiedTrust).isEqualTo(35 + 30 + 16 + 10); // rating: 25 × (3.5 − 1) / 4 = 15.6 -> 16
        assertThat(verifiedTrust - unverifiedTrust).isEqualTo(35);
        assertThat(part(SuitabilityScorer.score(prefs(), unverified, NOW), "trust").explanation()).contains("Owner identity not checked");
    }

    // ---------------- Required case 7: missing or stale availability ----------------

    @Test
    void staleOrMissingAvailabilityLosesFreshnessPoints() {
        ListingFacts g = goodListing();
        ListingFacts aging = new ListingFacts(g.status(), g.genderPolicy(), g.rooms(), 0, g.amenityAnswers(), NAMES, true,
                NOW.minus(Duration.ofDays(20)), 0, 0, 0, g.commute());
        ListingFacts stale = new ListingFacts(g.status(), g.genderPolicy(), g.rooms(), 0, g.amenityAnswers(), NAMES, true,
                NOW.minus(Duration.ofDays(45)), 0, 0, 0, g.commute());
        ListingFacts never = new ListingFacts(g.status(), g.genderPolicy(), g.rooms(), 0, g.amenityAnswers(), NAMES, true,
                null, 0, 0, 0, g.commute());
        assertThat(part(SuitabilityScorer.score(prefs(), aging, NOW), "trust").score()).isEqualTo(35 + 15 + 16 + 10);
        assertThat(part(SuitabilityScorer.score(prefs(), stale, NOW), "trust").score()).isEqualTo(35 + 0 + 16 + 10);
        assertThat(part(SuitabilityScorer.score(prefs(), never, NOW), "trust").explanation()).contains("never confirmed");
    }

    @Test
    void fullListingsAreExcluded() {
        ListingFacts g = goodListing();
        ListingFacts full = new ListingFacts(ListingStatus.PUBLISHED_UNAVAILABLE, g.genderPolicy(),
                List.of(new RoomFacts(1L, RoomType.DOUBLE, 9_000, 0)), 0, g.amenityAnswers(), NAMES, true,
                g.lastConfirmedAt(), 0, 0, 0, g.commute());
        Result r = SuitabilityScorer.score(prefs(), full, NOW);
        assertThat(r.eligible()).isFalse();
        assertThat(r.exclusions()).containsExactly(Exclusion.FULL);
    }

    // ---------------- Other rules ----------------

    @Test
    void genderRestrictedListingsAreExcludedOnlyWhenGendersConflict() {
        ListingFacts g = goodListing();
        ListingFacts menOnly = new ListingFacts(g.status(), GenderPolicy.MALE_ONLY, g.rooms(), 0, g.amenityAnswers(), NAMES,
                true, g.lastConfirmedAt(), 0, 0, 0, g.commute());
        assertThat(SuitabilityScorer.score(prefs(), menOnly, NOW).exclusions()).containsExactly(Exclusion.GENDER_POLICY);

        Preferences unknownGender = new Preferences(10_000, 40, List.of(RoomType.DOUBLE), Set.of(), Set.of(), null);
        Result r = SuitabilityScorer.score(unknownGender, menOnly, NOW);
        assertThat(r.eligible()).isTrue();
        assertThat(r.notes()).contains("Men-only accommodation.");
    }

    @Test
    void totalIsTheWeightedSumOfTheDisplayedParts() {
        Result r = SuitabilityScorer.score(prefs(), goodListing(), NOW);
        int byHand = (int) Math.round(r.components().stream().mapToDouble(c -> c.weight() * c.score()).sum() / 100.0);
        assertThat(r.totalScore()).isEqualTo(byHand);
        // budget 100, commute 100, room 100, amenities 100, trust 91 -> 98.65 -> 99
        assertThat(r.totalScore()).isEqualTo(99);
        assertThat(r.band()).isEqualTo("Excellent match");
        assertThat(r.components()).extracting(Component::weight).containsExactly(30, 25, 15, 15, 15);
    }

    @Test
    void reviewsMoveTheRatingButOneReviewCannotDominate() {
        ListingFacts g = goodListing();
        ListingFacts oneFiveStar = new ListingFacts(g.status(), g.genderPolicy(), g.rooms(), 0, g.amenityAnswers(), NAMES,
                true, g.lastConfirmedAt(), 1, 5.0, 0, g.commute());
        // (3 × 3.5 + 5) / 4 = 3.875 -> 25 × 2.875 / 4 = 18
        assertThat(part(SuitabilityScorer.score(prefs(), oneFiveStar, NOW), "trust").score()).isEqualTo(35 + 30 + 18 + 10);
    }

    @Test
    void bandsMatchTheDocumentedThresholds() {
        assertThat(SuitabilityScorer.band(85)).isEqualTo("Excellent match");
        assertThat(SuitabilityScorer.band(84)).isEqualTo("Good match");
        assertThat(SuitabilityScorer.band(70)).isEqualTo("Good match");
        assertThat(SuitabilityScorer.band(69)).isEqualTo("Fair match");
        assertThat(SuitabilityScorer.band(50)).isEqualTo("Fair match");
        assertThat(SuitabilityScorer.band(49)).isEqualTo("Weak match");
    }

    @Test
    void weightsAddUpToOneHundred() {
        assertThat(SuitabilityScorer.W_BUDGET + SuitabilityScorer.W_COMMUTE + SuitabilityScorer.W_ROOM
                + SuitabilityScorer.W_AMENITIES + SuitabilityScorer.W_TRUST).isEqualTo(100);
    }
}
