package com.campusnest.recommendation;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.campusnest.commute.CommuteEstimate;
import com.campusnest.listing.ListingStatus;
import com.campusnest.listing.ListingTypes.GenderPolicy;
import com.campusnest.listing.ListingTypes.RoomType;
import com.campusnest.student.Gender;

/**
 * Plain data records used by {@link SuitabilityScorer}. They hold only facts, no database objects, so the scorer
 * can be tested with hand-written examples.
 */
public final class SuitabilityModel {

    private SuitabilityModel() {
    }

    // ---------------- inputs ----------------

    /** What the student wants. roomChoices is ranked (first = most preferred); empty = no preference. */
    public record Preferences(int budgetMax, int maxCommuteMinutes, List<RoomType> roomChoices, Set<String> mustHave,
            Set<String> niceToHave, Gender gender) {
    }

    public record RoomFacts(Long roomId, RoomType type, int monthlyRent, int bedsAvailable) {
    }

    /**
     * What we know about a listing.
     *
     * @param amenityAnswers code -> true (yes) / false (no); a missing code means "owner did not say"
     * @param commute        may be null if the listing has no location (cannot happen for published listings)
     */
    public record ListingFacts(ListingStatus status, GenderPolicy genderPolicy, List<RoomFacts> rooms,
            int mandatoryMonthlyCharges, Map<String, Boolean> amenityAnswers, Map<String, String> amenityNames,
            boolean ownerVerified, Instant lastConfirmedAt, int reviewCount, double reviewRatingSum,
            int upheldReportsLast90Days, CommuteEstimate commute) {
    }

    // ---------------- outputs ----------------

    public enum Exclusion {
        FULL("No free beds right now"),
        GENDER_POLICY("Not open to your gender"),
        OVER_BUDGET("Every room is more than 30% over your budget");

        private final String text;

        Exclusion(String text) {
            this.text = text;
        }

        public String text() {
            return text;
        }
    }

    /** One part of the score: e.g. key "budget", weight 30, score 100, plus a plain-English reason. */
    public record Component(String key, String label, int weight, int score, String explanation) {
    }

    public record Result(
            boolean eligible,
            List<Exclusion> exclusions,
            Long bestRoomId,
            RoomType bestRoomType,
            Integer monthlyCost,
            Integer totalScore,
            String band,
            List<Component> components,
            int dataCompleteness,
            List<String> notes,
            CommuteEstimate commute) {
    }
}
