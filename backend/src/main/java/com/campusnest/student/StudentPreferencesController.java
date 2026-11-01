package com.campusnest.student;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusnest.auth.AppUserPrincipal;
import com.campusnest.auth.UserAccountRepository;
import com.campusnest.common.error.ApiException;
import com.campusnest.commute.TravelMode;
import com.campusnest.listing.Amenity;
import com.campusnest.listing.AmenityRepository;
import com.campusnest.listing.ListingTypes.RoomType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** The logged-in student's accommodation preferences (STUDENT area). */
@RestController
@RequestMapping("/api/v1/student/preferences")
public class StudentPreferencesController {

    private final StudentPreferencesRepository preferences;
    private final UserAccountRepository users;
    private final AmenityRepository amenities;

    public StudentPreferencesController(StudentPreferencesRepository preferences, UserAccountRepository users,
            AmenityRepository amenities) {
        this.preferences = preferences;
        this.users = users;
        this.amenities = amenities;
    }

    /** 204 No Content when the student has not set preferences yet. */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<PreferencesView> get(@AuthenticationPrincipal AppUserPrincipal me) {
        return preferences.findById(me.getId())
                .map(p -> ResponseEntity.ok(PreferencesView.from(p)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping
    @Transactional
    public PreferencesView save(@AuthenticationPrincipal AppUserPrincipal me, @Valid @RequestBody PreferencesRequest request) {
        Set<String> known = new HashSet<>(amenities.findAll().stream().map(Amenity::getCode).toList());
        List<String> must = request.mustHaveAmenities().stream().distinct().toList();
        List<String> nice = request.niceToHaveAmenities().stream().distinct().filter(c -> !must.contains(c)).toList();
        if (!known.containsAll(must) || !known.containsAll(nice)) {
            throw ApiException.badRequest("UNKNOWN_AMENITY", "One of the amenities does not exist.");
        }
        List<String> rooms = request.roomChoices().stream().distinct().map(RoomType::name).toList();

        // Fill in the values BEFORE the first save: the database rejects a row with no budget (CHECK constraint).
        StudentPreferences p = preferences.findById(me.getId())
                .orElseGet(() -> new StudentPreferences(users.getReferenceById(me.getId())));
        p.update(request.budgetMax(), request.maxCommuteMinutes(), request.commuteMode(), rooms, must, nice);
        return PreferencesView.from(preferences.save(p));
    }

    public record PreferencesRequest(
            @NotNull @Min(1000) @Max(200000) Integer budgetMax,
            @NotNull @Min(5) @Max(180) Integer maxCommuteMinutes,
            @NotNull TravelMode commuteMode,
            /** Ranked; at most the four room types. Empty = no preference. */
            @NotNull @Size(max = 4) List<@NotNull RoomType> roomChoices,
            @NotNull @Size(max = 15) List<@NotNull String> mustHaveAmenities,
            @NotNull @Size(max = 15) List<@NotNull String> niceToHaveAmenities) {
    }

    public record PreferencesView(int budgetMax, int maxCommuteMinutes, TravelMode commuteMode, List<String> roomChoices,
            List<String> mustHaveAmenities, List<String> niceToHaveAmenities, Instant updatedAt) {

        static PreferencesView from(StudentPreferences p) {
            return new PreferencesView(p.getBudgetMax(), p.getMaxCommuteMinutes(), p.getCommuteMode(), p.getRoomChoices(),
                    p.getMustHaveAmenities(), p.getNiceToHaveAmenities(), p.getUpdatedAt());
        }
    }
}
