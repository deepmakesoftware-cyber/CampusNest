package com.campusnest.roommate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.campusnest.auth.UserAccount;
import com.campusnest.common.persistence.CsvListConverter;
import com.campusnest.roommate.RoommateTypes.FoodPreference;
import com.campusnest.roommate.RoommateTypes.SharingSize;
import com.campusnest.roommate.RoommateTypes.SleepSchedule;
import com.campusnest.roommate.RoommateTypes.Smoking;
import com.campusnest.roommate.RoommateTypes.StudyEnvironment;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** A student's opt-in roommate preferences. Visible to other students only while discoverable = true. */
@Entity
@Table(name = "roommate_profiles")
public class RoommateProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @Column(nullable = false)
    private boolean discoverable;

    @Column(name = "budget_min", nullable = false)
    private int budgetMin;

    @Column(name = "budget_max", nullable = false)
    private int budgetMax;

    @Convert(converter = CsvListConverter.class)
    @Column(name = "preferred_localities", nullable = false, length = 500)
    private List<String> preferredLocalities = new ArrayList<>();

    @Column(name = "anywhere_near_college", nullable = false)
    private boolean anywhereNearCollege;

    @Enumerated(EnumType.STRING)
    @Column(name = "sleep_schedule", nullable = false, length = 20)
    private SleepSchedule sleepSchedule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Smoking smoking;

    @Column(name = "cannot_live_with_smoker", nullable = false)
    private boolean cannotLiveWithSmoker;

    @Enumerated(EnumType.STRING)
    @Column(name = "study_environment", nullable = false, length = 20)
    private StudyEnvironment studyEnvironment;

    @Column(nullable = false)
    private int cleanliness;

    @Column(nullable = false)
    private int guests;

    @Enumerated(EnumType.STRING)
    @Column(name = "food_preference", length = 20)
    private FoodPreference foodPreference;

    @Enumerated(EnumType.STRING)
    @Column(name = "sharing_size", nullable = false, length = 20)
    private SharingSize sharingSize;

    @Column(name = "same_gender_only", nullable = false)
    private boolean sameGenderOnly;

    @Column(length = 300)
    private String about;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RoommateProfile() {
    }

    public RoommateProfile(UserAccount user) {
        this.user = user;
    }

    /** All answers at once (the form always sends a complete profile). */
    public void update(boolean discoverable, int budgetMin, int budgetMax, List<String> localities, boolean anywhere,
            SleepSchedule sleep, Smoking smoking, boolean cannotLiveWithSmoker, StudyEnvironment study, int cleanliness,
            int guests, FoodPreference food, SharingSize sharing, boolean sameGenderOnly, String about) {
        this.discoverable = discoverable;
        this.budgetMin = budgetMin;
        this.budgetMax = budgetMax;
        this.preferredLocalities = new ArrayList<>(localities);
        this.anywhereNearCollege = anywhere;
        this.sleepSchedule = sleep;
        this.smoking = smoking;
        this.cannotLiveWithSmoker = cannotLiveWithSmoker;
        this.studyEnvironment = study;
        this.cleanliness = cleanliness;
        this.guests = guests;
        this.foodPreference = food;
        this.sharingSize = sharing;
        this.sameGenderOnly = sameGenderOnly;
        this.about = (about == null || about.isBlank()) ? null : about.trim();
        this.updatedAt = Instant.now();
    }

    public Long getUserId() {
        return userId;
    }

    public UserAccount getUser() {
        return user;
    }

    public boolean isDiscoverable() {
        return discoverable;
    }

    public int getBudgetMin() {
        return budgetMin;
    }

    public int getBudgetMax() {
        return budgetMax;
    }

    public List<String> getPreferredLocalities() {
        return List.copyOf(preferredLocalities);
    }

    public boolean isAnywhereNearCollege() {
        return anywhereNearCollege;
    }

    public SleepSchedule getSleepSchedule() {
        return sleepSchedule;
    }

    public Smoking getSmoking() {
        return smoking;
    }

    public boolean isCannotLiveWithSmoker() {
        return cannotLiveWithSmoker;
    }

    public StudyEnvironment getStudyEnvironment() {
        return studyEnvironment;
    }

    public int getCleanliness() {
        return cleanliness;
    }

    public int getGuests() {
        return guests;
    }

    public FoodPreference getFoodPreference() {
        return foodPreference;
    }

    public SharingSize getSharingSize() {
        return sharingSize;
    }

    public boolean isSameGenderOnly() {
        return sameGenderOnly;
    }

    public String getAbout() {
        return about;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
