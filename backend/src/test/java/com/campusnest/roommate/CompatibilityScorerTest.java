package com.campusnest.roommate;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.campusnest.roommate.CompatibilityScorer.Dealbreaker;
import com.campusnest.roommate.CompatibilityScorer.Result;
import com.campusnest.roommate.CompatibilityScorer.Side;
import com.campusnest.roommate.RoommateTypes.FoodPreference;
import com.campusnest.roommate.RoommateTypes.SharingSize;
import com.campusnest.roommate.RoommateTypes.SleepSchedule;
import com.campusnest.roommate.RoommateTypes.Smoking;
import com.campusnest.roommate.RoommateTypes.StudyEnvironment;
import com.campusnest.student.Gender;

/** FR-2 test cases. Expected numbers can be recalculated by hand from docs/ALGORITHMS.md section 2. */
class CompatibilityScorerTest {

    /** A tidy, early-sleeping, vegetarian non-smoker looking around Matunga/Dadar for ₹8,000–11,000. */
    static Side asha() {
        return new Side(Gender.FEMALE, 8_000, 11_000, Set.of("Matunga", "Dadar"), false, SleepSchedule.EARLY,
                Smoking.NON_SMOKER, false, StudyEnvironment.QUIET, 4, 2, FoodPreference.VEG, SharingSize.ONE_ROOMMATE, false);
    }

    static Side with(Side s, java.util.function.UnaryOperator<Side> f) {
        return f.apply(s);
    }

    @Test
    void identicalPreferencesAreAHundredPercentMatch() {
        Result r = CompatibilityScorer.score(asha(), asha());
        assertThat(r.compatible()).isTrue();
        assertThat(r.percent()).isEqualTo(100);
        assertThat(r.differences()).isEmpty();
        assertThat(r.matches()).contains("Both early sleepers", "Both non-smokers");
        assertThat(r.factorsUsed()).isEqualTo(9);
        assertThat(r.summary()).startsWith("Strong match");
    }

    @Test
    void partiallyCompatibleStudentsGetAMixedScoreWithReasons() {
        Side meera = new Side(Gender.FEMALE, 9_000, 12_000, Set.of("Dadar", "Sion"), false, SleepSchedule.LATE,
                Smoking.NON_SMOKER, false, StudyEnvironment.LIVELY, 2, 4, FoodPreference.NON_VEG, SharingSize.EITHER, false);
        Result r = CompatibilityScorer.score(asha(), meera);
        // budget 67 (₹2,000 of ₹3,000), area 33 (1 of 3), sleep 0, clean 50, smoking 100, study 0,
        // guests 50, food 30, sharing 100 -> (1340 + 495 + 0 + 750 + 1000 + 0 + 250 + 150 + 500) / 100 = 44.85 -> 45
        assertThat(r.percent()).isEqualTo(45);
        assertThat(r.summary()).startsWith("Mixed match");
        assertThat(r.budgetOverlap()).isEqualTo("₹9,000–₹11,000");
        assertThat(r.matches()).contains("Both like Dadar");
        assertThat(r.differences()).contains("You are an early sleeper; they are a late sleeper", "Cleanliness: you 4/5, they 2/5");
    }

    @Test
    void budgetsThatDoNotOverlapAreRuledOut() {
        Side rich = new Side(Gender.FEMALE, 15_000, 20_000, Set.of(), true, SleepSchedule.EARLY, Smoking.NON_SMOKER,
                false, StudyEnvironment.QUIET, 4, 2, FoodPreference.VEG, SharingSize.ONE_ROOMMATE, false);
        Result r = CompatibilityScorer.score(asha(), rich);
        assertThat(r.compatible()).isFalse();
        assertThat(r.dealbreakers()).containsExactly(Dealbreaker.BUDGET);
        assertThat(r.percent()).isNull();
    }

    @Test
    void smokingDealbreakerRespectsEitherStudent() {
        Side noSmokersPlease = new Side(Gender.FEMALE, 8_000, 11_000, Set.of(), true, SleepSchedule.EARLY,
                Smoking.NON_SMOKER, true, StudyEnvironment.QUIET, 4, 2, null, SharingSize.EITHER, false);
        Side outdoorSmoker = new Side(Gender.FEMALE, 8_000, 11_000, Set.of(), true, SleepSchedule.EARLY,
                Smoking.OUTDOOR_SMOKER, false, StudyEnvironment.QUIET, 4, 2, null, SharingSize.EITHER, false);
        assertThat(CompatibilityScorer.score(noSmokersPlease, outdoorSmoker).dealbreakers()).containsExactly(Dealbreaker.SMOKING);
        assertThat(CompatibilityScorer.score(outdoorSmoker, noSmokersPlease).dealbreakers()).containsExactly(Dealbreaker.SMOKING);

        // Without the dealbreaker, an outdoor smoker and a non-smoker get half the smoking points.
        Result soft = CompatibilityScorer.score(asha(), outdoorSmoker);
        assertThat(soft.compatible()).isTrue();
        assertThat(soft.factors()).anySatisfy(f -> {
            assertThat(f.key()).isEqualTo("smoking");
            assertThat(f.score()).isEqualTo(50);
        });
    }

    @Test
    void sameGenderOnlyNeedsBothGendersKnownAndEqual() {
        Side sameOnly = new Side(Gender.FEMALE, 8_000, 11_000, Set.of(), true, SleepSchedule.EARLY, Smoking.NON_SMOKER,
                false, StudyEnvironment.QUIET, 4, 2, null, SharingSize.EITHER, true);
        Side male = new Side(Gender.MALE, 8_000, 11_000, Set.of(), true, SleepSchedule.EARLY, Smoking.NON_SMOKER,
                false, StudyEnvironment.QUIET, 4, 2, null, SharingSize.EITHER, false);
        Side unknown = new Side(null, 8_000, 11_000, Set.of(), true, SleepSchedule.EARLY, Smoking.NON_SMOKER,
                false, StudyEnvironment.QUIET, 4, 2, null, SharingSize.EITHER, false);
        assertThat(CompatibilityScorer.score(sameOnly, male).dealbreakers()).containsExactly(Dealbreaker.GENDER);
        assertThat(CompatibilityScorer.score(sameOnly, unknown).dealbreakers()).containsExactly(Dealbreaker.GENDER);
        assertThat(CompatibilityScorer.score(sameOnly, asha()).compatible()).isTrue();
    }

    @Test
    void missingOptionalAnswerIsSkippedAndWeightsRebalanced() {
        Side noFood = new Side(Gender.FEMALE, 8_000, 11_000, Set.of("Matunga", "Dadar"), false, SleepSchedule.EARLY,
                Smoking.NON_SMOKER, false, StudyEnvironment.QUIET, 4, 2, null, SharingSize.ONE_ROOMMATE, false);
        Result r = CompatibilityScorer.score(asha(), noFood);
        assertThat(r.factorsUsed()).isEqualTo(8);
        assertThat(r.factors()).noneMatch(f -> f.key().equals("food"));
        assertThat(r.percent()).isEqualTo(100); // not penalised for the unanswered optional question
    }

    @Test
    void scoreIsTheSameWhicheverStudentIsViewing() {
        Side meera = new Side(Gender.FEMALE, 9_500, 13_000, Set.of("Sion"), false, SleepSchedule.REGULAR,
                Smoking.NON_SMOKER, false, StudyEnvironment.MODERATE, 3, 3, FoodPreference.EGGETARIAN, SharingSize.TWO_OR_MORE, false);
        assertThat(CompatibilityScorer.score(asha(), meera).percent()).isEqualTo(CompatibilityScorer.score(meera, asha()).percent());
    }

    @Test
    void scalesAndOrderedAnswersFollowTheDocumentedPoints() {
        assertThat(CompatibilityScorer.ordinalScore(0, 0)).isEqualTo(100);
        assertThat(CompatibilityScorer.ordinalScore(0, 1)).isEqualTo(50);
        assertThat(CompatibilityScorer.ordinalScore(0, 2)).isZero();
        assertThat(CompatibilityScorer.scaleScore(5, 1)).isZero();
        assertThat(CompatibilityScorer.scaleScore(4, 3)).isEqualTo(75);
        assertThat(CompatibilityScorer.foodScore(FoodPreference.VEG, FoodPreference.EGGETARIAN)).isEqualTo(70);
        assertThat(CompatibilityScorer.foodScore(FoodPreference.VEG, FoodPreference.NON_VEG)).isEqualTo(30);
        assertThat(CompatibilityScorer.foodScore(FoodPreference.NO_PREFERENCE, FoodPreference.NON_VEG)).isEqualTo(100);
    }

    @Test
    void namesAreShortenedForPrivacy() {
        assertThat(RoommateService.shortName("Asha Kulkarni (sample)")).isEqualTo("Asha K.");
        assertThat(RoommateService.shortName("Rohan Prakash Mehta")).isEqualTo("Rohan M.");
        assertThat(RoommateService.shortName("Meera")).isEqualTo("Meera");
    }
}
