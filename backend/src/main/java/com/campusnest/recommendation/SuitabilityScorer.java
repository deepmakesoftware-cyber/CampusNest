package com.campusnest.recommendation;

import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

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
 * The Accommodation Suitability Score (faculty requirement FR-1). A transparent, rule-based formula, documented
 * in docs/ALGORITHMS.md section 1. This class has no database or web code: same inputs -> same output, which is
 * what makes it easy to test and to explain.
 *
 * Total = (30 × budget + 25 × commute + 15 × room type + 15 × amenities + 15 × trust) / 100, each part 0–100.
 * The total is computed from the ROUNDED part scores, so anyone can check it by hand from the breakdown.
 */
public final class SuitabilityScorer {

    // ---- weights (must add up to 100) ----
    public static final int W_BUDGET = 30;
    public static final int W_COMMUTE = 25;
    public static final int W_ROOM = 15;
    public static final int W_AMENITIES = 15;
    public static final int W_TRUST = 15;

    /** A room more than 30% over budget is not shown at all. */
    static final double STRETCH = 0.30;

    // ---- trust sub-scores ----
    static final int TRUST_VERIFIED = 35;
    static final int TRUST_FRESH = 30;
    static final int TRUST_AGING = 15;
    static final int TRUST_RATING_MAX = 25;
    static final int TRUST_NO_REPORTS = 10;
    static final int FRESH_DAYS = 14;
    static final int AGING_DAYS = 30;
    /** Bayesian average: behave as if every listing already had 3 reviews of 3.5 stars. */
    static final double PRIOR_REVIEWS = 3;
    static final double PRIOR_RATING = 3.5;

    private static final NumberFormat INR = NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN"));

    private SuitabilityScorer() {
    }

    public static Result score(Preferences p, ListingFacts l, Instant now) {
        List<Exclusion> exclusions = new ArrayList<>();
        List<String> notes = new ArrayList<>();

        // ---------- 1. Eligibility ----------
        if (l.status() != ListingStatus.PUBLISHED_AVAILABLE
                || l.rooms().stream().noneMatch(r -> r.bedsAvailable() > 0)) {
            exclusions.add(Exclusion.FULL);
        }
        if (!genderAllowed(l.genderPolicy(), p.gender())) {
            exclusions.add(Exclusion.GENDER_POLICY);
        } else if (p.gender() == null && l.genderPolicy() != GenderPolicy.ANY) {
            notes.add(l.genderPolicy() == GenderPolicy.FEMALE_ONLY ? "Women-only accommodation." : "Men-only accommodation.");
        }
        double ceiling = p.budgetMax() * (1 + STRETCH);
        List<RoomFacts> candidates = l.rooms().stream()
                .filter(r -> r.bedsAvailable() > 0)
                .filter(r -> r.monthlyRent() + l.mandatoryMonthlyCharges() <= ceiling)
                .toList();
        if (candidates.isEmpty() && !exclusions.contains(Exclusion.FULL)) {
            exclusions.add(Exclusion.OVER_BUDGET);
        }
        if (!exclusions.isEmpty()) {
            return new Result(false, exclusions, null, null, null, null, null, List.of(), 0, notes, l.commute());
        }

        // ---------- 2. Listing-level parts (same for every room) ----------
        Component commute = commute(p, l);
        Component amenities = amenities(p, l);
        Component trust = trust(l, now);
        if (l.mandatoryMonthlyCharges() == 0) {
            notes.add("No mandatory extra charges are listed by the owner.");
        }
        if (l.commute() != null && l.commute().approximate()) {
            notes.add("Commute time is an approximate estimate.");
        }

        // ---------- 3. Score every eligible room; keep the best ----------
        record Scored(RoomFacts room, int cost, Component budget, Component roomType, int total) {
        }
        Scored best = candidates.stream().map(room -> {
            int cost = room.monthlyRent() + l.mandatoryMonthlyCharges();
            Component budget = budget(p, room, l.mandatoryMonthlyCharges());
            Component roomType = roomType(p, room.type());
            int total = (int) Math.round((W_BUDGET * budget.score() + W_COMMUTE * commute.score()
                    + W_ROOM * roomType.score() + W_AMENITIES * amenities.score() + W_TRUST * trust.score()) / 100.0);
            return new Scored(room, cost, budget, roomType, total);
        }).max(Comparator.comparingInt(Scored::total).thenComparing(Comparator.comparingInt(Scored::cost).reversed()))
                .orElseThrow();

        return new Result(true, List.of(), best.room().roomId(), best.room().type(), best.cost(), best.total(),
                band(best.total()), List.of(best.budget(), commute, best.roomType(), amenities, trust),
                dataCompleteness(p, l), notes, l.commute());
    }

    public static String band(int total) {
        if (total >= 85) {
            return "Excellent match";
        }
        if (total >= 70) {
            return "Good match";
        }
        return total >= 50 ? "Fair match" : "Weak match";
    }

    static boolean genderAllowed(GenderPolicy policy, Gender gender) {
        if (gender == null || policy == GenderPolicy.ANY) {
            return true; // unknown gender: shown, with a note
        }
        return (policy == GenderPolicy.FEMALE_ONLY && gender == Gender.FEMALE)
                || (policy == GenderPolicy.MALE_ONLY && gender == Gender.MALE);
    }

    // ---------------- Budget (30%) ----------------

    static Component budget(Preferences p, RoomFacts room, int charges) {
        int cost = room.monthlyRent() + charges;
        int max = p.budgetMax();
        String costText = charges > 0
                ? "₹" + INR.format(room.monthlyRent()) + " rent + ₹" + INR.format(charges) + " mandatory charges = ₹" + INR.format(cost)
                : "₹" + INR.format(cost);
        int score;
        String text;
        if (cost <= max) {
            score = 100;
            text = costText + " per month, within your ₹" + INR.format(max) + " budget.";
        } else {
            double over = (cost - max) / (double) max;
            score = (int) Math.round(Math.max(0, 100 * (1 - over / STRETCH)));
            text = costText + " per month is ₹" + INR.format(cost - max) + " (" + Math.round(over * 100)
                    + "%) over your ₹" + INR.format(max) + " budget.";
        }
        return new Component("budget", "Budget", W_BUDGET, score, text);
    }

    // ---------------- Commute (25%) ----------------

    static Component commute(Preferences p, ListingFacts l) {
        if (l.commute() == null) {
            return new Component("commute", "Commute", W_COMMUTE, 50, "No location available; a neutral score is used.");
        }
        int t = l.commute().minutes();
        int limit = p.maxCommuteMinutes();
        double half = limit / 2.0;
        double score;
        if (t <= half) {
            score = 100;
        } else if (t <= limit) {
            score = 100 - 30 * (t - half) / half;            // 100 -> 70
        } else if (t <= 1.5 * limit) {
            score = 70 - 70 * (t - limit) / (0.5 * limit);   // 70 -> 0
        } else {
            score = 0;
        }
        String mode = switch (l.commute().mode()) {
            case WALK -> "walking";
            case AUTO_RICKSHAW -> "auto-rickshaw";
            case BUS -> "bus";
            case LOCAL_TRAIN -> "local train";
            case ANY -> "the quickest option";
        };
        String verdict = t <= limit ? "within your " + limit + "-minute limit" : (t - limit) + " minutes over your " + limit + "-minute limit";
        String text = "About " + t + " min by " + mode + " (" + l.commute().distanceKm() + " km), " + verdict + "."
                + (l.commute().approximate() ? " Approximate estimate." : "");
        return new Component("commute", "Commute", W_COMMUTE, (int) Math.round(score), text);
    }

    // ---------------- Room type (15%) ----------------

    static Component roomType(Preferences p, RoomType type) {
        List<RoomType> choices = p.roomChoices();
        if (choices.isEmpty()) {
            return new Component("roomType", "Room type", W_ROOM, 100, "You have no room-type preference.");
        }
        int rank = choices.indexOf(type);
        int[] points = {100, 70, 40};
        String name = type.name().charAt(0) + type.name().substring(1).toLowerCase();
        if (rank < 0 || rank >= points.length) {
            return new Component("roomType", "Room type", W_ROOM, 0, name + " is not one of your preferred room types.");
        }
        String ordinal = switch (rank) {
            case 0 -> "1st";
            case 1 -> "2nd";
            default -> "3rd";
        };
        return new Component("roomType", "Room type", W_ROOM, points[rank], name + " is your " + ordinal + " choice.");
    }

    // ---------------- Amenities (15%) ----------------

    static Component amenities(Preferences p, ListingFacts l) {
        if (p.mustHave().isEmpty() && p.niceToHave().isEmpty()) {
            return new Component("amenities", "Amenities", W_AMENITIES, 100, "You did not select any amenities.");
        }
        double earned = 0;
        double possible = 0;
        List<String> metMust = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        List<String> unknown = new ArrayList<>();
        for (String code : p.mustHave()) {
            possible += 2;
            earned += 2 * credit(l, code, metMust, missing, unknown);
        }
        List<String> metNice = new ArrayList<>();
        for (String code : p.niceToHave()) {
            possible += 1;
            earned += credit(l, code, metNice, missing, unknown);
        }
        int score = (int) Math.round(100 * earned / possible);
        StringBuilder text = new StringBuilder();
        if (!p.mustHave().isEmpty()) {
            text.append("Has ").append(metMust.size()).append(" of your ").append(p.mustHave().size()).append(" must-haves");
            if (!metMust.isEmpty()) {
                text.append(" (").append(String.join(", ", metMust)).append(")");
            }
            text.append(". ");
        }
        if (!metNice.isEmpty()) {
            text.append("Also has: ").append(String.join(", ", metNice)).append(". ");
        }
        if (!missing.isEmpty()) {
            text.append("Not available: ").append(String.join(", ", missing)).append(". ");
        }
        if (!unknown.isEmpty()) {
            text.append("Not specified by owner (half credit): ").append(String.join(", ", unknown)).append(".");
        }
        return new Component("amenities", "Amenities", W_AMENITIES, score, text.toString().trim());
    }

    /** yes = 1, unknown = 0.5, no = 0. Also sorts the amenity's name into the right list for the explanation. */
    private static double credit(ListingFacts l, String code, List<String> met, List<String> missing, List<String> unknown) {
        String name = l.amenityNames().getOrDefault(code, code);
        Boolean answer = l.amenityAnswers().get(code);
        if (answer == null) {
            unknown.add(name);
            return 0.5;
        }
        if (answer) {
            met.add(name);
            return 1;
        }
        missing.add(name);
        return 0;
    }

    // ---------------- Trust (15%) ----------------

    static Component trust(ListingFacts l, Instant now) {
        int verified = l.ownerVerified() ? TRUST_VERIFIED : 0;

        Optional<Long> days = Optional.ofNullable(l.lastConfirmedAt()).map(t -> Duration.between(t, now).toDays());
        int fresh = days.map(d -> d <= FRESH_DAYS ? TRUST_FRESH : d <= AGING_DAYS ? TRUST_AGING : 0).orElse(0);

        double bayes = (PRIOR_REVIEWS * PRIOR_RATING + l.reviewRatingSum()) / (PRIOR_REVIEWS + l.reviewCount());
        int rating = (int) Math.round(TRUST_RATING_MAX * (bayes - 1) / 4);

        int reports = l.upheldReportsLast90Days() == 0 ? TRUST_NO_REPORTS : 0;

        String text = (l.ownerVerified() ? "Owner identity checked" : "Owner identity not checked") + " (" + verified + "/" + TRUST_VERIFIED + ") · "
                + days.map(d -> "availability confirmed " + (d == 0 ? "today" : d + (d == 1 ? " day" : " days") + " ago"))
                        .orElse("availability never confirmed") + " (" + fresh + "/" + TRUST_FRESH + ") · "
                + (l.reviewCount() == 0 ? "no reviews yet, neutral rating used"
                        : l.reviewCount() + (l.reviewCount() == 1 ? " review" : " reviews") + ", adjusted rating "
                                + String.format(Locale.ROOT, "%.1f", bayes) + "/5") + " (" + rating + "/" + TRUST_RATING_MAX + ") · "
                + (reports == TRUST_NO_REPORTS ? "no upheld reports" : "recent upheld reports") + " (" + reports + "/" + TRUST_NO_REPORTS + ")";
        return new Component("trust", "Trust", W_TRUST, verified + fresh + rating + reports, text);
    }

    // ---------------- Data completeness ----------------

    /** How much of what matters to THIS student is actually known (not assumed). */
    static int dataCompleteness(Preferences p, ListingFacts l) {
        double known = l.commute() == null ? 0 : (l.commute().approximate() ? 0.5 : 1);
        double total = 1;
        for (String code : concat(p.mustHave(), p.niceToHave())) {
            total += 1;
            if (l.amenityAnswers().containsKey(code)) {
                known += 1;
            }
        }
        return (int) Math.round(100 * known / total);
    }

    private static List<String> concat(java.util.Set<String> a, java.util.Set<String> b) {
        List<String> all = new ArrayList<>(a);
        all.addAll(b);
        return all;
    }
}
