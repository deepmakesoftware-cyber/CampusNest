package com.campusnest.recommendation;

import java.time.Instant;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusnest.college.College;
import com.campusnest.common.error.ApiException;
import com.campusnest.commute.CommuteService;
import com.campusnest.listing.Amenity;
import com.campusnest.listing.AmenityRepository;
import com.campusnest.listing.Listing;
import com.campusnest.listing.ListingAmenity;
import com.campusnest.listing.ListingCharge;
import com.campusnest.listing.ListingRepository;
import com.campusnest.listing.ListingTypes.ChargeFrequency;
import com.campusnest.listing.ListingTypes.GenderPolicy;
import com.campusnest.listing.ListingTypes.PropertyType;
import com.campusnest.listing.ListingTypes.RoomType;
import com.campusnest.listing.ListingViewMapper;
import com.campusnest.listing.PublicListingService;
import com.campusnest.owner.OwnerProfile;
import com.campusnest.owner.OwnerProfileRepository;
import com.campusnest.owner.VerificationStatus;
import com.campusnest.recommendation.SuitabilityModel.Exclusion;
import com.campusnest.recommendation.SuitabilityModel.ListingFacts;
import com.campusnest.recommendation.SuitabilityModel.Preferences;
import com.campusnest.recommendation.SuitabilityModel.Result;
import com.campusnest.recommendation.SuitabilityModel.RoomFacts;
import com.campusnest.student.StudentPreferences;
import com.campusnest.student.StudentPreferencesRepository;
import com.campusnest.student.StudentProfile;
import com.campusnest.student.StudentProfileRepository;

/**
 * Builds ranked, explained recommendations for one student from REAL stored listings:
 * load visible listings -> turn each into plain facts -> SuitabilityScorer -> sort by score.
 */
@Service
public class RecommendationService {

    static final int MAX_RESULTS = 50;

    private final StudentPreferencesRepository preferences;
    private final StudentProfileRepository profiles;
    private final ListingRepository listings;
    private final AmenityRepository amenities;
    private final OwnerProfileRepository ownerProfiles;
    private final CommuteService commute;
    private final TrustSignals trustSignals;

    public RecommendationService(StudentPreferencesRepository preferences, StudentProfileRepository profiles,
            ListingRepository listings, AmenityRepository amenities, OwnerProfileRepository ownerProfiles,
            CommuteService commute, TrustSignals trustSignals) {
        this.preferences = preferences;
        this.profiles = profiles;
        this.listings = listings;
        this.amenities = amenities;
        this.ownerProfiles = ownerProfiles;
        this.commute = commute;
        this.trustSignals = trustSignals;
    }

    @Transactional(readOnly = true)
    public Recommendations recommend(Long studentId) {
        Context ctx = context(studentId);
        Instant now = Instant.now();

        Map<Exclusion, Integer> excluded = new EnumMap<>(Exclusion.class);
        List<RecommendedListing> ranked = listings.findVisibleToStudents().stream()
                .map(l -> {
                    Result r = SuitabilityScorer.score(ctx.prefs(), facts(l, ctx), now);
                    if (!r.eligible()) {
                        r.exclusions().forEach(e -> excluded.merge(e, 1, Integer::sum));
                    }
                    return toView(l, r, ctx);
                })
                .filter(v -> v.result().eligible())
                .sorted(Comparator.comparing((RecommendedListing v) -> v.result().totalScore()).reversed()
                        .thenComparing(v -> v.result().monthlyCost())
                        .thenComparing(v -> v.result().commute() == null ? Integer.MAX_VALUE : v.result().commute().minutes()))
                .limit(MAX_RESULTS)
                .toList();

        Map<String, Integer> excludedByReason = excluded.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().text(), Map.Entry::getValue));
        return new Recommendations(ranked, excludedByReason, ctx.college().getShortName(), now);
    }

    /** The breakdown for one listing (shown on the listing page), including why it is excluded if it is. */
    @Transactional(readOnly = true)
    public RecommendedListing forListing(Long studentId, Long listingId) {
        Context ctx = context(studentId);
        Listing l = listings.findById(listingId)
                .filter(PublicListingService::isVisible)
                .orElseThrow(() -> ApiException.notFound("LISTING_NOT_FOUND", "Listing not found."));
        return toView(l, SuitabilityScorer.score(ctx.prefs(), facts(l, ctx), Instant.now()), ctx);
    }

    // ---------------- helpers ----------------

    private record Context(Preferences prefs, College college, Map<String, String> amenityNames,
            Map<Long, VerificationStatus> ownerVerification, StudentPreferences raw) {
    }

    private Context context(Long studentId) {
        StudentProfile profile = profiles.findById(studentId)
                .orElseThrow(() -> ApiException.notFound("PROFILE_NOT_FOUND", "Student profile not found."));
        if (profile.getCollege() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "COLLEGE_REQUIRED",
                    "Choose your college in your profile first, so we can estimate your commute.");
        }
        StudentPreferences p = preferences.findById(studentId).orElseThrow(() -> new ApiException(HttpStatus.CONFLICT,
                "PREFERENCES_REQUIRED", "Set your accommodation preferences first."));
        Preferences prefs = new Preferences(p.getBudgetMax(), p.getMaxCommuteMinutes(),
                p.getRoomChoices().stream().map(RoomType::valueOf).toList(),
                new LinkedHashSet<>(p.getMustHaveAmenities()), new LinkedHashSet<>(p.getNiceToHaveAmenities()),
                profile.getGender());
        Map<String, String> names = amenities.findAll().stream().collect(Collectors.toMap(Amenity::getCode, Amenity::getName));
        Map<Long, VerificationStatus> verification = ownerProfiles.findAll().stream()
                .collect(Collectors.toMap(OwnerProfile::getUserId, OwnerProfile::getVerificationStatus));
        return new Context(prefs, profile.getCollege(), names, verification, p);
    }

    private ListingFacts facts(Listing l, Context ctx) {
        int mandatoryMonthly = l.getCharges().stream()
                .filter(c -> c.isMandatory() && c.getFrequency() == ChargeFrequency.MONTHLY)
                .mapToInt(ListingCharge::getAmount).sum();
        Map<String, Boolean> answers = l.getAmenityAnswers().stream()
                .collect(Collectors.toMap(a -> a.getAmenity().getCode(), ListingAmenity::isAvailable));
        TrustSignals.Signals trust = trustSignals.forListing(l.getId());
        return new ListingFacts(l.getStatus(), l.getGenderPolicy(),
                l.getRooms().stream().map(r -> new RoomFacts(r.getId(), r.getRoomType(), r.getMonthlyRent(), r.getBedsAvailable())).toList(),
                mandatoryMonthly, answers, ctx.amenityNames(),
                ctx.ownerVerification().get(l.getOwner().getId()) == VerificationStatus.VERIFIED,
                l.getLastConfirmedAt(), trust.reviewCount(), trust.reviewRatingSum(), trust.upheldReportsLast90Days(),
                commute.estimate(l, ctx.college(), ctx.raw().getCommuteMode()));
    }

    private RecommendedListing toView(Listing l, Result r, Context ctx) {
        String cover = l.getImages().isEmpty() ? null : ListingViewMapper.IMAGE_URL_PREFIX + l.getImages().get(0).getId();
        return new RecommendedListing(l.getId(), l.getTitle(), l.getLocality(), l.getPropertyType(), l.getGenderPolicy(),
                cover, l.isSampleData(), ctx.ownerVerification().getOrDefault(l.getOwner().getId(), VerificationStatus.UNVERIFIED), r);
    }

    // ---------------- DTOs ----------------

    public record RecommendedListing(Long listingId, String title, String locality, PropertyType propertyType,
            GenderPolicy genderPolicy, String coverImageUrl, boolean sampleData, VerificationStatus ownerVerification,
            Result result) {
    }

    /** excludedByReason: how many listings were left out, and why (shown to the student for transparency). */
    public record Recommendations(List<RecommendedListing> items, Map<String, Integer> excludedByReason,
            String collegeShortName, Instant generatedAt) {
    }
}
