package com.campusnest.roommate;

import java.time.Instant;
import java.util.List;

import com.campusnest.roommate.CompatibilityScorer.Result;
import com.campusnest.roommate.RoommateTypes.FoodPreference;
import com.campusnest.roommate.RoommateTypes.RequestStatus;
import com.campusnest.roommate.RoommateTypes.SharingSize;
import com.campusnest.roommate.RoommateTypes.SleepSchedule;
import com.campusnest.roommate.RoommateTypes.Smoking;
import com.campusnest.roommate.RoommateTypes.StudyEnvironment;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class RoommateDtos {

    private RoommateDtos() {
    }

    public record ProfileRequest(
            @NotNull Boolean discoverable,
            @NotNull @Min(1000) @Max(200000) Integer budgetMin,
            @NotNull @Min(1000) @Max(200000) Integer budgetMax,
            @NotNull @Size(max = 10) List<@NotNull @Size(max = 40) String> preferredLocalities,
            @NotNull Boolean anywhereNearCollege,
            @NotNull SleepSchedule sleepSchedule,
            @NotNull Smoking smoking,
            @NotNull Boolean cannotLiveWithSmoker,
            @NotNull StudyEnvironment studyEnvironment,
            @NotNull @Min(1) @Max(5) Integer cleanliness,
            @NotNull @Min(1) @Max(5) Integer guests,
            /** optional: leave out to skip this factor */
            FoodPreference foodPreference,
            @NotNull SharingSize sharingSize,
            @NotNull Boolean sameGenderOnly,
            @Size(max = 300) String about) {
    }

    public record ProfileView(boolean discoverable, int budgetMin, int budgetMax, List<String> preferredLocalities,
            boolean anywhereNearCollege, SleepSchedule sleepSchedule, Smoking smoking, boolean cannotLiveWithSmoker,
            StudyEnvironment studyEnvironment, int cleanliness, int guests, FoodPreference foodPreference,
            SharingSize sharingSize, boolean sameGenderOnly, String about, Instant updatedAt) {

        static ProfileView from(RoommateProfile p) {
            return new ProfileView(p.isDiscoverable(), p.getBudgetMin(), p.getBudgetMax(), p.getPreferredLocalities(),
                    p.isAnywhereNearCollege(), p.getSleepSchedule(), p.getSmoking(), p.isCannotLiveWithSmoker(),
                    p.getStudyEnvironment(), p.getCleanliness(), p.getGuests(), p.getFoodPreference(), p.getSharingSize(),
                    p.isSameGenderOnly(), p.getAbout(), p.getUpdatedAt());
        }
    }

    /**
     * What one student may see about another BEFORE they are connected: no full name, email, phone, gender or exact
     * budget (only the overlap, inside the compatibility result).
     */
    public record PersonPreview(Long userId, String shortName, String college, String course, Short yearOfStudy,
            String about, SleepSchedule sleepSchedule, StudyEnvironment studyEnvironment, int cleanliness, int guests,
            Smoking smoking, FoodPreference foodPreference, SharingSize sharingSize, List<String> preferredLocalities,
            boolean anywhereNearCollege) {
    }

    public enum Relationship {
        NONE,
        REQUEST_SENT,
        REQUEST_RECEIVED,
        CONNECTED
    }

    public record MatchView(PersonPreview person, Result compatibility, Relationship relationship, Long requestId) {
    }

    public record SendRequest(@NotNull Long toUserId, @Size(max = 500) String message) {
    }

    /** direction: SENT or RECEIVED, from the viewer's point of view. */
    public record RequestView(Long id, String direction, RequestStatus status, String message, Instant createdAt,
            PersonPreview person) {
    }

    /** Only for ACCEPTED connections: full name and contact details. */
    public record ConnectionView(Long requestId, Long userId, String displayName, String email, String phone,
            String college, String course, Instant connectedAt) {
    }
}
