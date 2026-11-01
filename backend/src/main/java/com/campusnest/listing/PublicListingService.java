package com.campusnest.listing;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusnest.auth.AccountStatus;
import com.campusnest.college.College;
import com.campusnest.college.CollegeRepository;
import com.campusnest.common.error.ApiException;
import com.campusnest.common.geo.GeoDistance;
import com.campusnest.common.web.PageResponse;
import com.campusnest.listing.ListingDtos.ListingDetail;
import com.campusnest.listing.ListingTypes.GenderPolicy;
import com.campusnest.listing.PublicListingDtos.Freshness;
import com.campusnest.listing.PublicListingDtos.ListingCard;
import com.campusnest.listing.PublicListingDtos.OwnerPublic;
import com.campusnest.listing.PublicListingDtos.PublicListingDetail;
import com.campusnest.listing.PublicListingDtos.SearchCriteria;
import com.campusnest.listing.PublicListingDtos.SortOrder;
import com.campusnest.listing.PublicListingDtos.SuitableFor;
import com.campusnest.owner.OwnerProfile;
import com.campusnest.owner.OwnerProfileRepository;
import com.campusnest.owner.VerificationStatus;

/**
 * Student-facing search and listing pages. Results come only from stored, published listings of active owners.
 *
 * Design note: filtering happens in Java after loading the visible listings. At CampusNest's scale (hundreds of
 * listings in one city) this is fast and much easier to read and test than a dynamic SQL query. If listings ever
 * reach many thousands, the filters can move into the database query without changing the API.
 */
@Service
public class PublicListingService {

    static final int PAGE_SIZE = 12;
    static final int FRESH_DAYS = 14;
    static final int AGING_DAYS = 30;

    private final ListingRepository listings;
    private final CollegeRepository colleges;
    private final OwnerProfileRepository ownerProfiles;
    private final ListingViewMapper mapper;

    public PublicListingService(ListingRepository listings, CollegeRepository colleges,
            OwnerProfileRepository ownerProfiles, ListingViewMapper mapper) {
        this.listings = listings;
        this.colleges = colleges;
        this.ownerProfiles = ownerProfiles;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<ListingCard> search(SearchCriteria c) {
        Optional<College> college = Optional.ofNullable(c.collegeId()).flatMap(colleges::findById);
        if (c.collegeId() != null && college.isEmpty()) {
            throw ApiException.badRequest("UNKNOWN_COLLEGE", "The selected college does not exist.");
        }
        Set<String> wantedAmenities = c.amenities() == null ? Set.of()
                : c.amenities().stream().map(s -> s.toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
        String text = (c.q() == null || c.q().isBlank()) ? null : c.q().trim().toLowerCase(Locale.ROOT);

        List<Listing> visible = listings.findVisibleToStudents();
        Map<Long, VerificationStatus> verification = ownerVerification(visible);

        List<ListingCard> cards = visible.stream()
                .filter(l -> text == null || contains(l.getTitle(), text) || contains(l.getLocality(), text))
                .filter(l -> genderAllows(l.getGenderPolicy(), c.suitableFor()))
                .filter(l -> !c.availableOnly() || l.getStatus() == ListingStatus.PUBLISHED_AVAILABLE)
                .filter(l -> hasAllAmenities(l, wantedAmenities))
                .map(l -> toCard(l, c, college.orElse(null), verification))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(card -> c.maxDistanceKm() == null || card.distanceKm() == null
                        || card.distanceKm() <= c.maxDistanceKm())
                .sorted(comparator(c.sort(), college.isPresent()))
                .toList();

        int page = Math.max(c.page(), 0);
        int from = Math.min(page * PAGE_SIZE, cards.size());
        int to = Math.min(from + PAGE_SIZE, cards.size());
        int totalPages = (cards.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        return new PageResponse<>(cards.subList(from, to), page, PAGE_SIZE, cards.size(), totalPages);
    }

    @Transactional(readOnly = true)
    public PublicListingDetail get(Long id) {
        Listing l = listings.findById(id)
                .filter(PublicListingService::isVisible)
                .orElseThrow(() -> ApiException.notFound("LISTING_NOT_FOUND", "Listing not found."));
        OwnerProfile profile = ownerProfiles.findById(l.getOwner().getId()).orElse(null);
        OwnerPublic owner = new OwnerPublic(l.getOwner().getDisplayName(),
                profile == null ? null : profile.getOwnerType(),
                profile == null ? null : profile.getBusinessName(),
                profile == null ? VerificationStatus.UNVERIFIED : profile.getVerificationStatus());
        ListingDetail full = mapper.toDetail(l); // reuse its room/charge/amenity/photo views
        return new PublicListingDetail(l.getId(), l.getTitle(), l.getDescription(), l.getPropertyType(),
                l.getGenderPolicy(), l.getAddressLine(), l.getLocality(), l.getCity(), l.getPincode(),
                l.getLatitude(), l.getLongitude(), l.getStatus(), l.getPublishedAt(), l.getLastConfirmedAt(),
                freshness(l.getLastConfirmedAt()), l.isSampleData(),
                full.rooms(), full.charges(), full.amenities(), full.images(), owner);
    }

    /** Used by other modules (saved listings, enquiries) to check a listing can be shown to students. */
    public static boolean isVisible(Listing l) {
        return l.getStatus().isPublished() && l.getOwner().getStatus() == AccountStatus.ACTIVE;
    }

    public static Freshness freshness(Instant lastConfirmedAt) {
        if (lastConfirmedAt == null) {
            return Freshness.STALE;
        }
        long days = Duration.between(lastConfirmedAt, Instant.now()).toDays();
        if (days <= FRESH_DAYS) {
            return Freshness.FRESH;
        }
        return days <= AGING_DAYS ? Freshness.AGING : Freshness.STALE;
    }

    // ---------------- helpers ----------------

    private Optional<ListingCard> toCard(Listing l, SearchCriteria c, College college,
            Map<Long, VerificationStatus> verification) {
        // Only rooms that match BOTH the room type and the budget count, so "from ₹X" is honest.
        List<RoomOption> matchingRooms = l.rooms().stream()
                .filter(r -> c.roomType() == null || r.getRoomType() == c.roomType())
                .filter(r -> c.maxRent() == null || r.getMonthlyRent() <= c.maxRent())
                .filter(r -> !c.availableOnly() || r.getBedsAvailable() > 0)
                .toList();
        if (matchingRooms.isEmpty()) {
            return Optional.empty();
        }
        Double distance = null;
        if (college != null && l.getLatitude() != null && l.getLongitude() != null) {
            distance = GeoDistance.rounded(GeoDistance.kilometres(l.getLatitude(), l.getLongitude(),
                    college.getLatitude(), college.getLongitude()));
        }
        String cover = l.imageList().isEmpty() ? null : ListingViewMapper.IMAGE_URL_PREFIX + l.imageList().get(0).getId();
        return Optional.of(new ListingCard(l.getId(), l.getTitle(), l.getLocality(), l.getPropertyType(),
                l.getGenderPolicy(),
                matchingRooms.stream().mapToInt(RoomOption::getMonthlyRent).min().orElseThrow(),
                l.rooms().stream().map(RoomOption::getRoomType).distinct().toList(),
                l.totalBedsAvailable(), l.getLatitude(), l.getLongitude(), distance, cover,
                freshness(l.getLastConfirmedAt()), l.getLastConfirmedAt(),
                verification.getOrDefault(l.getOwner().getId(), VerificationStatus.UNVERIFIED),
                l.isSampleData()));
    }

    private Map<Long, VerificationStatus> ownerVerification(List<Listing> visible) {
        Set<Long> ownerIds = visible.stream().map(l -> l.getOwner().getId()).collect(Collectors.toSet());
        return ownerProfiles.findAllById(ownerIds).stream()
                .collect(Collectors.toMap(OwnerProfile::getUserId, OwnerProfile::getVerificationStatus));
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    static boolean genderAllows(GenderPolicy policy, SuitableFor suitableFor) {
        if (suitableFor == null || policy == GenderPolicy.ANY) {
            return true;
        }
        return (suitableFor == SuitableFor.FEMALE && policy == GenderPolicy.FEMALE_ONLY)
                || (suitableFor == SuitableFor.MALE && policy == GenderPolicy.MALE_ONLY);
    }

    /** "Must have" amenities: the owner must have answered YES (unknown does not count). */
    private static boolean hasAllAmenities(Listing l, Set<String> wanted) {
        if (wanted.isEmpty()) {
            return true;
        }
        Set<String> available = l.amenityAnswers().stream()
                .filter(ListingAmenity::isAvailable)
                .map(a -> a.getAmenity().getCode())
                .collect(Collectors.toSet());
        return available.containsAll(wanted);
    }

    private static Comparator<ListingCard> comparator(SortOrder sort, boolean hasCollege) {
        SortOrder effective = (sort == null) ? (hasCollege ? SortOrder.DISTANCE_ASC : SortOrder.RENT_ASC) : sort;
        Comparator<ListingCard> byRent = Comparator.comparingInt(ListingCard::fromRent);
        return switch (effective) {
            case RENT_ASC -> byRent.thenComparing(ListingCard::id);
            case DISTANCE_ASC -> Comparator.comparing(ListingCard::distanceKm,
                    Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(byRent);
            case RECENTLY_CONFIRMED -> Comparator.comparing(ListingCard::lastConfirmedAt,
                    Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(byRent);
        };
    }
}
