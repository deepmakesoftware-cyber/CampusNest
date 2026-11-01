package com.campusnest.listing;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusnest.auth.UserAccount;
import com.campusnest.auth.UserAccountRepository;
import com.campusnest.common.error.ApiException;
import com.campusnest.common.storage.ImageProcessor;
import com.campusnest.common.storage.ImageProcessor.ProcessedImage;
import com.campusnest.common.storage.ImageStorage;
import com.campusnest.listing.ListingDtos.AmenityAnswer;
import com.campusnest.listing.ListingDtos.AvailabilityRequest;
import com.campusnest.listing.ListingDtos.ChargeRequest;
import com.campusnest.listing.ListingDtos.ListingDetail;
import com.campusnest.listing.ListingDtos.ListingRequest;
import com.campusnest.listing.ListingDtos.ListingSummary;
import com.campusnest.listing.ListingDtos.RoomAvailability;
import com.campusnest.listing.ListingDtos.RoomRequest;

/**
 * Everything an owner can do with their OWN listings. Every method starts with loadOwn(), which finds the
 * listing by id AND owner id: another owner's listing id simply returns 404, so owners cannot read or change
 * each other's listings even by guessing ids (prevents IDOR attacks).
 */
@Service
public class OwnerListingService {

    static final int MAX_IMAGES = 10;

    private final ListingRepository listings;
    private final UserAccountRepository users;
    private final ListingLifecycleService lifecycle;
    private final ListingViewMapper mapper;
    private final ImageProcessor imageProcessor;
    private final ImageStorage imageStorage;

    public OwnerListingService(ListingRepository listings, UserAccountRepository users,
            ListingLifecycleService lifecycle, ListingViewMapper mapper, ImageProcessor imageProcessor,
            ImageStorage imageStorage) {
        this.listings = listings;
        this.users = users;
        this.lifecycle = lifecycle;
        this.mapper = mapper;
        this.imageProcessor = imageProcessor;
        this.imageStorage = imageStorage;
    }

    @Transactional(readOnly = true)
    public List<ListingSummary> list(Long ownerId) {
        return listings.findByOwnerIdOrderByUpdatedAtDesc(ownerId).stream().map(mapper::toSummary).toList();
    }

    @Transactional
    public ListingDetail create(Long ownerId, String title) {
        UserAccount owner = users.getReferenceById(ownerId);
        Listing listing = listings.save(new Listing(owner, title));
        lifecycle.recordCreated(listing, owner);
        return mapper.toDetail(listing);
    }

    @Transactional(readOnly = true)
    public ListingDetail get(Long ownerId, Long listingId) {
        return mapper.toDetail(loadOwn(ownerId, listingId));
    }

    /**
     * Saves the whole edit form. If the listing is already published and the owner changed something a reviewer
     * checked (title, description, address, location, type, gender policy, amenities), it goes back to review.
     * Rent, charges and bed counts can change without re-review.
     */
    @Transactional
    public ListingDetail update(Long ownerId, Long listingId, ListingRequest request) {
        Listing listing = loadOwn(ownerId, listingId);
        requireEditable(listing);

        boolean material = listing.updateDetails(request.title(), request.description(), request.propertyType(),
                request.genderPolicy(), request.addressLine(), request.locality(), request.pincode(),
                request.latitude(), request.longitude());
        syncRooms(listing, request.rooms());
        replaceCharges(listing, request.charges());
        material |= applyAmenityAnswers(listing, request.amenities());

        if (material && listing.getStatus().isPublished()) {
            lifecycle.transition(listing, ListingStatus.PENDING_VERIFICATION, users.getReferenceById(ownerId),
                    "Owner edited the listing; it needs to be reviewed again.");
        }
        return mapper.toDetail(listing);
    }

    @Transactional
    public ListingDetail submit(Long ownerId, Long listingId) {
        Listing listing = loadOwn(ownerId, listingId);
        ListingLifecycle.requireTransition(listing.getStatus(), ListingStatus.PENDING_VERIFICATION);
        Map<String, String> missing = SubmissionChecklist.missingItems(listing,
                List.copyOf(mapper.amenityCatalogue().values()));
        if (!missing.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "LISTING_INCOMPLETE",
                    "Complete the listing before submitting it for review.", missing);
        }
        // Submitting is the owner's statement that rooms and availability are correct right now.
        listing.confirmAvailability(Instant.now());
        lifecycle.transition(listing, ListingStatus.PENDING_VERIFICATION, users.getReferenceById(ownerId), null);
        return mapper.toDetail(listing);
    }

    /**
     * Owner updates (or simply re-confirms) free beds. This keeps the listing "fresh" for students and
     * switches it between Available and Unavailable automatically.
     */
    @Transactional
    public ListingDetail updateAvailability(Long ownerId, Long listingId, AvailabilityRequest request) {
        Listing listing = loadOwn(ownerId, listingId);
        if (!listing.getStatus().isPublished()) {
            throw ApiException.conflict("LISTING_NOT_PUBLISHED",
                    "Availability can be updated once the listing is published. Edit the rooms instead.");
        }
        Map<Long, RoomOption> roomsById = listing.rooms().stream()
                .collect(Collectors.toMap(RoomOption::getId, Function.identity()));
        Instant now = Instant.now();
        for (RoomAvailability update : request.rooms()) {
            RoomOption room = roomsById.get(update.roomId());
            if (room == null) {
                throw ApiException.badRequest("UNKNOWN_ROOM", "One of the rooms does not belong to this listing.");
            }
            if (update.bedsAvailable() > room.getBedsTotal()) {
                throw ApiException.badRequest("TOO_MANY_BEDS", "Available beds cannot be more than total beds.");
            }
            room.updateAvailability(update.bedsAvailable(), now);
        }
        listing.confirmAvailability(now);

        ListingStatus target = listing.totalBedsAvailable() > 0
                ? ListingStatus.PUBLISHED_AVAILABLE
                : ListingStatus.PUBLISHED_UNAVAILABLE;
        if (target != listing.getStatus()) {
            lifecycle.transition(listing, target, users.getReferenceById(ownerId), null);
        }
        return mapper.toDetail(listing);
    }

    /** Only drafts can be deleted; anything that was submitted is kept for the audit trail. */
    @Transactional
    public void delete(Long ownerId, Long listingId) {
        Listing listing = loadOwn(ownerId, listingId);
        if (listing.getStatus() != ListingStatus.DRAFT) {
            throw ApiException.conflict("LISTING_NOT_DELETABLE", "Only draft listings can be deleted.");
        }
        List<String> keys = listing.imageList().stream().map(ListingImage::getStorageKey).toList();
        listings.delete(listing);
        keys.forEach(imageStorage::delete);
    }

    @Transactional
    public ListingDetail addImage(Long ownerId, Long listingId, byte[] fileBytes) {
        Listing listing = loadOwn(ownerId, listingId);
        requireEditable(listing);
        if (listing.imageList().size() >= MAX_IMAGES) {
            throw ApiException.conflict("TOO_MANY_IMAGES", "A listing can have at most " + MAX_IMAGES + " photos.");
        }
        ProcessedImage image = imageProcessor.process(fileBytes);
        String key = imageStorage.save(image.data(), image.extension());
        listing.imageList().add(new ListingImage(listing, key, image.contentType(), image.data().length,
                image.width(), image.height()));
        listing.touch();
        sendBackToReviewIfPublished(listing, ownerId, "Owner changed the photos; the listing needs to be reviewed again.");
        return mapper.toDetail(listing);
    }

    @Transactional
    public ListingDetail deleteImage(Long ownerId, Long listingId, Long imageId) {
        Listing listing = loadOwn(ownerId, listingId);
        requireEditable(listing);
        ListingImage image = listing.imageList().stream()
                .filter(i -> i.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("IMAGE_NOT_FOUND", "Photo not found."));
        listing.imageList().remove(image);
        listing.touch();
        imageStorage.delete(image.getStorageKey());
        sendBackToReviewIfPublished(listing, ownerId, "Owner changed the photos; the listing needs to be reviewed again.");
        return mapper.toDetail(listing);
    }

    // ---------------- helpers ----------------

    private Listing loadOwn(Long ownerId, Long listingId) {
        return listings.findByIdAndOwnerId(listingId, ownerId)
                .orElseThrow(() -> ApiException.notFound("LISTING_NOT_FOUND", "Listing not found."));
    }

    private static void requireEditable(Listing listing) {
        ListingStatus status = listing.getStatus();
        if (status == ListingStatus.PENDING_VERIFICATION) {
            throw ApiException.conflict("LISTING_UNDER_REVIEW",
                    "This listing is being reviewed and cannot be edited until the review is finished.");
        }
        if (!ListingLifecycle.isOwnerEditable(status)) {
            throw ApiException.conflict("LISTING_LOCKED", "This listing can no longer be edited.");
        }
    }

    private void sendBackToReviewIfPublished(Listing listing, Long ownerId, String reason) {
        if (listing.getStatus().isPublished()) {
            lifecycle.transition(listing, ListingStatus.PENDING_VERIFICATION, users.getReferenceById(ownerId), reason);
        }
    }

    /** Update rooms that kept their id, add new ones, remove the rest. Keeps room ids stable for availability updates. */
    private static void syncRooms(Listing listing, List<RoomRequest> requested) {
        Map<Long, RoomOption> existing = listing.rooms().stream()
                .collect(Collectors.toMap(RoomOption::getId, Function.identity()));
        Set<Long> keep = requested.stream().map(RoomRequest::id).filter(id -> id != null).collect(Collectors.toSet());
        for (Long id : keep) {
            if (!existing.containsKey(id)) {
                throw ApiException.badRequest("UNKNOWN_ROOM", "One of the rooms does not belong to this listing.");
            }
        }
        listing.rooms().removeIf(room -> !keep.contains(room.getId()));

        Instant now = Instant.now();
        for (RoomRequest r : requested) {
            RoomOption room = (r.id() == null) ? new RoomOption(listing) : existing.get(r.id());
            room.update(r.roomType(), r.sharingCapacity(), r.monthlyRent(), r.securityDeposit(), r.bedsTotal(),
                    r.bedsAvailable(), now);
            if (r.id() == null) {
                listing.rooms().add(room);
            }
        }
    }

    private static void replaceCharges(Listing listing, List<ChargeRequest> requested) {
        listing.charges().clear();
        for (ChargeRequest c : requested) {
            listing.charges().add(new ListingCharge(listing, c.chargeType(), c.description(), c.amount(),
                    c.frequency(), c.mandatory()));
        }
    }

    /**
     * Applies yes/no answers in place (update existing rows, add new, remove omitted). Updating in place avoids
     * a unique-constraint clash that "delete all then re-insert" would cause. Returns true if anything changed.
     */
    private boolean applyAmenityAnswers(Listing listing, List<AmenityAnswer> requested) {
        Map<Long, Amenity> catalogue = mapper.amenityCatalogue();
        Map<Long, Boolean> wanted = new HashMap<>();
        for (AmenityAnswer a : requested) {
            if (!catalogue.containsKey(a.amenityId())) {
                throw ApiException.badRequest("UNKNOWN_AMENITY", "One of the amenities does not exist.");
            }
            wanted.put(a.amenityId(), a.available());
        }
        Map<Long, Boolean> before = listing.amenityAnswers().stream()
                .collect(Collectors.toMap(a -> a.getAmenity().getId(), ListingAmenity::isAvailable));

        listing.amenityAnswers().removeIf(a -> !wanted.containsKey(a.getAmenity().getId()));
        for (ListingAmenity answer : listing.amenityAnswers()) {
            answer.setAvailable(wanted.get(answer.getAmenity().getId()));
        }
        for (Map.Entry<Long, Boolean> entry : wanted.entrySet()) {
            if (!before.containsKey(entry.getKey())) {
                listing.amenityAnswers().add(new ListingAmenity(listing, catalogue.get(entry.getKey()), entry.getValue()));
            }
        }
        return !before.equals(wanted);
    }
}
