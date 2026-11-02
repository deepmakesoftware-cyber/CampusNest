package com.campusnest.roommate;

/** Answer choices for roommate profiles. Each list is ordered, so "adjacent" answers are next to each other. */
public final class RoommateTypes {

    private RoommateTypes() {
    }

    public enum SleepSchedule {
        EARLY,
        REGULAR,
        LATE
    }

    public enum Smoking {
        NON_SMOKER,
        OUTDOOR_SMOKER,
        SMOKER
    }

    public enum StudyEnvironment {
        QUIET,
        MODERATE,
        LIVELY
    }

    public enum FoodPreference {
        VEG,
        EGGETARIAN,
        NON_VEG,
        VEGAN,
        NO_PREFERENCE
    }

    public enum SharingSize {
        ONE_ROOMMATE,
        TWO_OR_MORE,
        EITHER
    }

    public enum RequestStatus {
        PENDING,
        ACCEPTED,
        DECLINED,
        WITHDRAWN
    }
}
