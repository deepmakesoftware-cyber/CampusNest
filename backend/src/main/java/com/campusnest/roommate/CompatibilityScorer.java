package com.campusnest.roommate;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.campusnest.roommate.RoommateTypes.FoodPreference;
import com.campusnest.roommate.RoommateTypes.SharingSize;
import com.campusnest.roommate.RoommateTypes.SleepSchedule;
import com.campusnest.roommate.RoommateTypes.Smoking;
import com.campusnest.roommate.RoommateTypes.StudyEnvironment;
import com.campusnest.student.Gender;

/**
 * Roommate compatibility (faculty requirement FR-2), documented in docs/ALGORITHMS.md section 2.
 * Pure Java: two students' stated preferences in, a percentage plus reasons out. The result is the same whichever
 * student is "you" (symmetric), except that the wording is written from the viewer's point of view.
 *
 * A number cannot guarantee two people will get along; the UI always says so.
 */
public final class CompatibilityScorer {

    static final int W_BUDGET = 20;
    static final int W_LOCATION = 15;
    static final int W_SLEEP = 15;
    static final int W_CLEAN = 15;
    static final int W_SMOKING = 10;
    static final int W_STUDY = 10;
    static final int W_GUESTS = 5;
    static final int W_FOOD = 5;
    static final int W_SHARING = 5;
    static final int TOTAL_FACTORS = 9;

    private static final NumberFormat INR = NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN"));

    private CompatibilityScorer() {
    }

    /** One student's answers (gender comes from their student profile and may be null). */
    public record Side(Gender gender, int budgetMin, int budgetMax, Set<String> localities, boolean anywhere,
            SleepSchedule sleep, Smoking smoking, boolean cannotLiveWithSmoker, StudyEnvironment study, int cleanliness,
            int guests, FoodPreference food, SharingSize sharing, boolean sameGenderOnly) {

        public static Side of(RoommateProfile p, Gender gender) {
            return new Side(gender, p.getBudgetMin(), p.getBudgetMax(), new HashSet<>(p.getPreferredLocalities()),
                    p.isAnywhereNearCollege(), p.getSleepSchedule(), p.getSmoking(), p.isCannotLiveWithSmoker(),
                    p.getStudyEnvironment(), p.getCleanliness(), p.getGuests(), p.getFoodPreference(), p.getSharingSize(),
                    p.isSameGenderOnly());
        }
    }

    public enum Dealbreaker {
        GENDER("One of you wants a roommate of the same gender."),
        SMOKING("One of you cannot live with a smoker."),
        BUDGET("Your budgets do not overlap at all.");

        private final String text;

        Dealbreaker(String text) {
            this.text = text;
        }

        public String text() {
            return text;
        }
    }

    public record Factor(String key, String label, int weight, int score) {
    }

    public record Result(boolean compatible, List<Dealbreaker> dealbreakers, Integer percent, List<Factor> factors,
            List<String> matches, List<String> differences, String summary, int factorsUsed, int factorsTotal,
            String budgetOverlap) {
    }

    /** a = the student viewing, b = the other student. */
    public static Result score(Side a, Side b) {
        List<Dealbreaker> dealbreakers = new ArrayList<>();
        if ((a.sameGenderOnly() || b.sameGenderOnly()) && (a.gender() == null || b.gender() == null || a.gender() != b.gender())) {
            dealbreakers.add(Dealbreaker.GENDER);
        }
        if ((a.cannotLiveWithSmoker() && b.smoking() != Smoking.NON_SMOKER)
                || (b.cannotLiveWithSmoker() && a.smoking() != Smoking.NON_SMOKER)) {
            dealbreakers.add(Dealbreaker.SMOKING);
        }
        int overlapLow = Math.max(a.budgetMin(), b.budgetMin());
        int overlapHigh = Math.min(a.budgetMax(), b.budgetMax());
        if (overlapHigh < overlapLow) {
            dealbreakers.add(Dealbreaker.BUDGET);
        }
        if (!dealbreakers.isEmpty()) {
            return new Result(false, dealbreakers, null, List.of(), List.of(), List.of(), null, 0, TOTAL_FACTORS, null);
        }

        List<Factor> factors = new ArrayList<>();
        List<String> matches = new ArrayList<>();
        List<String> differences = new ArrayList<>();
        String overlapText = "₹" + INR.format(overlapLow) + "–₹" + INR.format(overlapHigh);

        // Budget (20): how much of the narrower range is shared.
        int narrower = Math.min(a.budgetMax() - a.budgetMin(), b.budgetMax() - b.budgetMin());
        int budget = narrower == 0 ? 100 : (int) Math.round(Math.min(100, 100.0 * (overlapHigh - overlapLow) / narrower));
        factors.add(new Factor("budget", "Budget", W_BUDGET, budget));
        (budget >= 50 ? matches : differences).add("Budgets overlap at " + overlapText + " per month");

        // Location (15): shared preferred areas, or one of you is open to anywhere near college.
        int location;
        if (a.anywhere() || b.anywhere() || a.localities().isEmpty() || b.localities().isEmpty()) {
            location = 100;
            matches.add("Flexible about the area near college");
        } else {
            Set<String> shared = new HashSet<>(a.localities());
            shared.retainAll(b.localities());
            Set<String> union = new HashSet<>(a.localities());
            union.addAll(b.localities());
            location = (int) Math.round(100.0 * shared.size() / union.size());
            if (shared.isEmpty()) {
                differences.add("No preferred area in common");
            } else {
                matches.add("Both like " + String.join(", ", shared.stream().sorted().toList()));
            }
        }
        factors.add(new Factor("location", "Preferred area", W_LOCATION, location));

        // Sleep (15)
        int sleep = ordinalScore(a.sleep().ordinal(), b.sleep().ordinal());
        factors.add(new Factor("sleep", "Sleep schedule", W_SLEEP, sleep));
        if (a.sleep() == b.sleep()) {
            matches.add("Both " + sleepWord(a.sleep()) + " sleepers");
        } else {
            differences.add("You are " + article(sleepWord(a.sleep())) + " sleeper; they are " + article(sleepWord(b.sleep())) + " sleeper");
        }

        // Cleanliness (15)
        int clean = scaleScore(a.cleanliness(), b.cleanliness());
        factors.add(new Factor("cleanliness", "Cleanliness", W_CLEAN, clean));
        if (Math.abs(a.cleanliness() - b.cleanliness()) <= 1) {
            matches.add("Similar cleanliness standards (" + a.cleanliness() + " and " + b.cleanliness() + " out of 5)");
        } else {
            differences.add("Cleanliness: you " + a.cleanliness() + "/5, they " + b.cleanliness() + "/5");
        }

        // Smoking (10)
        int smoking = smokingScore(a.smoking(), b.smoking());
        factors.add(new Factor("smoking", "Smoking", W_SMOKING, smoking));
        if (smoking == 100) {
            matches.add(a.smoking() == Smoking.NON_SMOKER ? "Both non-smokers" : "Both smoke");
        } else {
            differences.add("Smoking habits differ (" + smokingWord(a.smoking()) + " / " + smokingWord(b.smoking()) + ")");
        }

        // Study environment (10)
        int study = ordinalScore(a.study().ordinal(), b.study().ordinal());
        factors.add(new Factor("study", "Study environment", W_STUDY, study));
        if (a.study() == b.study()) {
            matches.add("Both prefer a " + a.study().name().toLowerCase() + " place to study");
        } else {
            differences.add("You prefer " + a.study().name().toLowerCase() + "; they prefer " + b.study().name().toLowerCase() + " surroundings for study");
        }

        // Guests (5)
        int guests = scaleScore(a.guests(), b.guests());
        factors.add(new Factor("guests", "Guests and socialising", W_GUESTS, guests));
        if (Math.abs(a.guests() - b.guests()) >= 2) {
            differences.add("Different comfort with guests (" + a.guests() + " vs " + b.guests() + " out of 5)");
        }

        // Food (5) - optional: only when both answered.
        int used = 8;
        if (a.food() != null && b.food() != null) {
            int food = foodScore(a.food(), b.food());
            factors.add(new Factor("food", "Food habits", W_FOOD, food));
            used++;
            if (food == 100) {
                matches.add("Compatible food habits");
            } else {
                differences.add("Food habits differ (" + foodWord(a.food()) + " / " + foodWord(b.food()) + ")");
            }
        }

        // Sharing size (5)
        int sharing = (a.sharing() == SharingSize.EITHER || b.sharing() == SharingSize.EITHER || a.sharing() == b.sharing()) ? 100 : 0;
        factors.add(new Factor("sharing", "Number of roommates", W_SHARING, sharing));
        if (sharing == 0) {
            differences.add("You want " + sharingWord(a.sharing()) + "; they want " + sharingWord(b.sharing()));
        }

        int weightSum = factors.stream().mapToInt(Factor::weight).sum();
        int percent = (int) Math.round(factors.stream().mapToDouble(f -> f.weight() * f.score()).sum() / weightSum);
        return new Result(true, List.of(), percent, factors, matches, differences, summary(percent), used, TOTAL_FACTORS, overlapText);
    }

    static String summary(int percent) {
        if (percent >= 80) {
            return "Strong match: you agree on most everyday habits.";
        }
        if (percent >= 60) {
            return "Good match, with a few differences worth discussing.";
        }
        if (percent >= 40) {
            return "Mixed match: talk through the differences before deciding.";
        }
        return "Many differences in everyday habits.";
    }

    /** For ordered three-step answers: same 100, neighbours 50, opposite ends 0. */
    static int ordinalScore(int a, int b) {
        return switch (Math.abs(a - b)) {
            case 0 -> 100;
            case 1 -> 50;
            default -> 0;
        };
    }

    /** For 1–5 scales: 100 − 25 per step of difference. */
    static int scaleScore(int a, int b) {
        return Math.max(0, 100 - 25 * Math.abs(a - b));
    }

    static int smokingScore(Smoking a, Smoking b) {
        boolean aSmokes = a != Smoking.NON_SMOKER;
        boolean bSmokes = b != Smoking.NON_SMOKER;
        if (aSmokes == bSmokes) {
            return 100;
        }
        Smoking smoker = aSmokes ? a : b;
        return smoker == Smoking.OUTDOOR_SMOKER ? 50 : 0;
    }

    static int foodScore(FoodPreference a, FoodPreference b) {
        if (a == b || a == FoodPreference.NO_PREFERENCE || b == FoodPreference.NO_PREFERENCE) {
            return 100;
        }
        boolean aVegFamily = a != FoodPreference.NON_VEG;
        boolean bVegFamily = b != FoodPreference.NON_VEG;
        return (aVegFamily && bVegFamily) ? 70 : 30;
    }

    private static String sleepWord(SleepSchedule s) {
        return switch (s) {
            case EARLY -> "early";
            case REGULAR -> "regular";
            case LATE -> "late";
        };
    }

    private static String article(String word) {
        return (word.startsWith("e") ? "an " : "a ") + word;
    }

    private static String smokingWord(Smoking s) {
        return switch (s) {
            case NON_SMOKER -> "non-smoker";
            case OUTDOOR_SMOKER -> "smokes outdoors only";
            case SMOKER -> "smoker";
        };
    }

    private static String foodWord(FoodPreference f) {
        return switch (f) {
            case VEG -> "vegetarian";
            case EGGETARIAN -> "eggetarian";
            case NON_VEG -> "non-vegetarian";
            case VEGAN -> "vegan";
            case NO_PREFERENCE -> "no preference";
        };
    }

    private static String sharingWord(SharingSize s) {
        return switch (s) {
            case ONE_ROOMMATE -> "one roommate";
            case TWO_OR_MORE -> "two or more roommates";
            case EITHER -> "any number of roommates";
        };
    }
}
