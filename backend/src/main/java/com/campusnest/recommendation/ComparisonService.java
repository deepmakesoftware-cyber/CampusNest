package com.campusnest.recommendation;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusnest.college.College;
import com.campusnest.common.error.ApiException;
import com.campusnest.common.geo.GeoDistance;
import com.campusnest.commute.CommuteService;
import com.campusnest.commute.CommuteService.CommuteOption;
import com.campusnest.commute.TravelMode;
import com.campusnest.listing.Listing;
import com.campusnest.listing.ListingAmenity;
import com.campusnest.listing.ListingCharge;
import com.campusnest.listing.ListingRepository;
import com.campusnest.listing.ListingTypes.ChargeFrequency;
import com.campusnest.listing.ListingTypes.RoomType;
import com.campusnest.listing.PublicListingService;
import com.campusnest.listing.RoomOption;
import com.campusnest.recommendation.SuitabilityModel.ListingFacts;
import com.campusnest.recommendation.SuitabilityModel.Preferences;
import com.campusnest.recommendation.SuitabilityModel.Result;
import com.campusnest.recommendation.SuitabilityModel.RoomFacts;
import com.campusnest.student.StudentPreferences;
import com.campusnest.student.StudentPreferencesRepository;
import com.campusnest.student.StudentProfile;
import com.campusnest.student.StudentProfileRepository;

/**
 * Cost-versus-commute comparison (faculty requirement FR-4): 2–4 listings side by side with rent, disclosed charges,
 * estimated travel time and estimated transport cost for every mode, and an estimated overall monthly cost.
 *
 * Honesty rules: every travel figure is an estimate and says how it was made; the overall monthly cost is only
 * calculated when every part of it is known; otherwise it is null with a note explaining what is missing.
 */
@Service
public class ComparisonService {

    static final int MIN_ITEMS = 2;
    static final int MAX_ITEMS = 4;
    static final int DEFAULT_MAX_COMMUTE = 45;

    private final ListingRepository listings;
    private final StudentProfileRepository profiles;
    private final StudentPreferencesRepository preferences;
    private final CommuteService commute;

    public ComparisonService(ListingRepository listings, StudentProfileRepository profiles,
            StudentPreferencesRepository preferences, CommuteService commute) {
        this.listings = listings;
        this.profiles = profiles;
        this.preferences = preferences;
        this.commute = commute;
    }

    @Transactional(readOnly = true)
    public Comparison compare(Long studentId, List<Long> ids, int daysPerMonth) {
        List<Long> unique = List.copyOf(new LinkedHashSet<>(ids));
        if (unique.size() < MIN_ITEMS || unique.size() > MAX_ITEMS) {
            throw ApiException.badRequest("INVALID_PARAMETER", "Choose between 2 and 4 different listings to compare.");
        }
        if (daysPerMonth < 1 || daysPerMonth > 31) {
            throw ApiException.badRequest("INVALID_PARAMETER", "College days per month must be between 1 and 31.");
        }
        StudentProfile profile = profiles.findById(studentId)
                .orElseThrow(() -> ApiException.notFound("PROFILE_NOT_FOUND", "Student profile not found."));
        College college = Optional.ofNullable(profile.getCollege()).orElseThrow(() -> new ApiException(HttpStatus.CONFLICT,
                "COLLEGE_REQUIRED", "Choose your college in your profile first, so we can estimate your commute."));
        Optional<StudentPreferences> prefs = preferences.findById(studentId);
        int maxCommute = prefs.map(StudentPreferences::getMaxCommuteMinutes).orElse(DEFAULT_MAX_COMMUTE);
        CommuteService.Context ctx = commute.context();

        List<ComparedListing> items = unique.stream().map(id -> {
            Listing l = listings.findById(id).filter(PublicListingService::isVisible)
                    .orElseThrow(() -> ApiException.notFound("LISTING_NOT_FOUND", "Listing " + id + " not found."));
            return compareOne(l, college, prefs, profile, maxCommute, daysPerMonth, ctx);
        }).toList();

        return new Comparison(items, daysPerMonth, maxCommute, college.getShortName(), Instant.now());
    }

    private ComparedListing compareOne(Listing l, College college, Optional<StudentPreferences> prefs,
            StudentProfile profile, int maxCommute, int days, CommuteService.Context ctx) {
        RoomOption room = chooseRoom(l, prefs, profile);
        int mandatoryMonthly = sum(l, true, ChargeFrequency.MONTHLY);
        int optionalMonthly = sum(l, false, ChargeFrequency.MONTHLY);
        int oneTime = l.getCharges().stream().filter(c -> c.getFrequency() == ChargeFrequency.ONE_TIME)
                .mapToInt(ListingCharge::getAmount).sum();

        List<CommuteOption> options = commute.options(ctx, l, college, days);
        CommuteOption suggested = suggest(options, maxCommute);
        Integer total = null;
        String totalNote;
        if (suggested == null) {
            totalNote = "No travel estimate is available for this listing.";
        } else if (suggested.monthlyCost() == null) {
            totalNote = "Incomplete: " + suggested.costNote();
        } else {
            total = room.getMonthlyRent() + mandatoryMonthly + suggested.monthlyCost();
            totalNote = "Estimated: rent + compulsory monthly charges + " + modeName(suggested.mode()) + " costs.";
        }
        double straight = GeoDistance.rounded(GeoDistance.kilometres(l.getLatitude(), l.getLongitude(),
                college.getLatitude(), college.getLongitude()));
        return new ComparedListing(l.getId(), l.getTitle(), l.getLocality(), l.isSampleData(), room.getRoomType(),
                room.getMonthlyRent(), room.getSecurityDeposit(), mandatoryMonthly, optionalMonthly, oneTime, straight,
                options, suggested == null ? null : suggested.mode(), total, totalNote);
    }

    /**
     * Suggested mode: among options within the student's commute limit, the cheapest one with a known cost
     * (ties: faster). If nothing is within the limit, the fastest available option.
     */
    static CommuteOption suggest(List<CommuteOption> options, int maxCommute) {
        List<CommuteOption> available = options.stream().filter(CommuteOption::available).toList();
        Optional<CommuteOption> withinLimit = available.stream()
                .filter(o -> o.minutes() <= maxCommute)
                .min(Comparator.comparing((CommuteOption o) -> o.monthlyCost() == null ? Integer.MAX_VALUE : o.monthlyCost())
                        .thenComparingInt(CommuteOption::minutes));
        return withinLimit.orElseGet(() -> available.stream().min(Comparator.comparingInt(CommuteOption::minutes)).orElse(null));
    }

    /** The student's best-fitting room (same rule as the suitability score), else the cheapest room with a free bed. */
    private static RoomOption chooseRoom(Listing l, Optional<StudentPreferences> prefs, StudentProfile profile) {
        if (prefs.isPresent()) {
            StudentPreferences p = prefs.get();
            Preferences sp = new Preferences(p.getBudgetMax(), p.getMaxCommuteMinutes(),
                    p.getRoomChoices().stream().map(RoomType::valueOf).toList(),
                    new LinkedHashSet<>(p.getMustHaveAmenities()), new LinkedHashSet<>(p.getNiceToHaveAmenities()),
                    profile.getGender());
            Map<String, Boolean> answers = l.getAmenityAnswers().stream()
                    .collect(Collectors.toMap(a -> a.getAmenity().getCode(), ListingAmenity::isAvailable));
            ListingFacts facts = new ListingFacts(l.getStatus(), l.getGenderPolicy(),
                    l.getRooms().stream().map(r -> new RoomFacts(r.getId(), r.getRoomType(), r.getMonthlyRent(), r.getBedsAvailable())).toList(),
                    sum(l, true, ChargeFrequency.MONTHLY), answers, Map.of(), false, null, 0, 0, 0, null);
            Result r = SuitabilityScorer.score(sp, facts, Instant.now());
            if (r.eligible()) {
                return l.getRooms().stream().filter(room -> room.getId().equals(r.bestRoomId())).findFirst().orElseThrow();
            }
        }
        return l.getRooms().stream()
                .sorted(Comparator.comparing((RoomOption room) -> room.getBedsAvailable() == 0)
                        .thenComparingInt(RoomOption::getMonthlyRent))
                .findFirst().orElseThrow();
    }

    private static int sum(Listing l, boolean mandatory, ChargeFrequency frequency) {
        return l.getCharges().stream()
                .filter(c -> c.isMandatory() == mandatory && c.getFrequency() == frequency)
                .mapToInt(ListingCharge::getAmount).sum();
    }

    private static String modeName(TravelMode mode) {
        return switch (mode) {
            case WALK -> "walking (free)";
            case AUTO_RICKSHAW -> "auto-rickshaw";
            case BUS -> "bus";
            case LOCAL_TRAIN -> "local train";
            case ANY -> "travel";
        };
    }

    // ---------------- DTOs ----------------

    public record ComparedListing(Long listingId, String title, String locality, boolean sampleData, RoomType roomType,
            int monthlyRent, Integer securityDeposit, int compulsoryMonthlyCharges, int optionalMonthlyCharges,
            int oneTimeCharges, double straightLineKm, List<CommuteOption> commuteOptions, TravelMode suggestedMode,
            /** null when any part is unknown (see totalNote) */
            Integer estimatedMonthlyTotal, String totalNote) {
    }

    public record Comparison(List<ComparedListing> items, int daysPerMonth, int maxCommuteMinutes,
            String collegeShortName, Instant generatedAt) {
    }
}
