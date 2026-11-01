package com.campusnest.student;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.campusnest.auth.UserAccount;
import com.campusnest.common.persistence.CsvListConverter;
import com.campusnest.commute.TravelMode;

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

/** What a student is looking for. Input to the suitability score (docs/ALGORITHMS.md section 1). */
@Entity
@Table(name = "student_preferences")
public class StudentPreferences {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @Column(name = "budget_max", nullable = false)
    private int budgetMax;

    @Column(name = "max_commute_minutes", nullable = false)
    private int maxCommuteMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "commute_mode", nullable = false, length = 20)
    private TravelMode commuteMode;

    /** Ranked: first = most preferred room type. */
    @Convert(converter = CsvListConverter.class)
    @Column(name = "room_choices", nullable = false, length = 100)
    private List<String> roomChoices = new ArrayList<>();

    @Convert(converter = CsvListConverter.class)
    @Column(name = "must_have_amenities", nullable = false, length = 500)
    private List<String> mustHaveAmenities = new ArrayList<>();

    @Convert(converter = CsvListConverter.class)
    @Column(name = "nice_to_have_amenities", nullable = false, length = 500)
    private List<String> niceToHaveAmenities = new ArrayList<>();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StudentPreferences() {
    }

    public StudentPreferences(UserAccount user) {
        this.user = user;
        this.commuteMode = TravelMode.ANY;
    }

    public void update(int budgetMax, int maxCommuteMinutes, TravelMode commuteMode, List<String> roomChoices,
            List<String> mustHave, List<String> niceToHave) {
        this.budgetMax = budgetMax;
        this.maxCommuteMinutes = maxCommuteMinutes;
        this.commuteMode = commuteMode;
        this.roomChoices = new ArrayList<>(roomChoices);
        this.mustHaveAmenities = new ArrayList<>(mustHave);
        this.niceToHaveAmenities = new ArrayList<>(niceToHave);
        this.updatedAt = Instant.now();
    }

    public Long getUserId() {
        return userId;
    }

    public int getBudgetMax() {
        return budgetMax;
    }

    public int getMaxCommuteMinutes() {
        return maxCommuteMinutes;
    }

    public TravelMode getCommuteMode() {
        return commuteMode;
    }

    public List<String> getRoomChoices() {
        return List.copyOf(roomChoices);
    }

    public List<String> getMustHaveAmenities() {
        return List.copyOf(mustHaveAmenities);
    }

    public List<String> getNiceToHaveAmenities() {
        return List.copyOf(niceToHaveAmenities);
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
