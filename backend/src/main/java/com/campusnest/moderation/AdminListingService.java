package com.campusnest.moderation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusnest.auth.UserAccount;
import com.campusnest.auth.UserAccountRepository;
import com.campusnest.common.error.ApiException;
import com.campusnest.common.web.PageResponse;
import com.campusnest.listing.Listing;
import com.campusnest.listing.ListingDtos.AdminListingDetail;
import com.campusnest.listing.ListingDtos.AdminListingSummary;
import com.campusnest.listing.ListingDtos.OwnerInfo;
import com.campusnest.listing.ListingLifecycleService;
import com.campusnest.listing.ListingRepository;
import com.campusnest.listing.ListingStatus;
import com.campusnest.listing.ListingViewMapper;
import com.campusnest.owner.OwnerProfile;
import com.campusnest.owner.OwnerProfileRepository;
import com.campusnest.owner.VerificationStatus;

/**
 * Administrator review of listings (FR-3). Drafts are the owner's private work in progress and are never shown
 * to administrators; everything from submission onwards is.
 */
@Service
public class AdminListingService {

    static final int PAGE_SIZE = 20;

    private final ListingRepository listings;
    private final UserAccountRepository users;
    private final OwnerProfileRepository ownerProfiles;
    private final ListingLifecycleService lifecycle;
    private final ListingViewMapper mapper;

    public AdminListingService(ListingRepository listings, UserAccountRepository users,
            OwnerProfileRepository ownerProfiles, ListingLifecycleService lifecycle, ListingViewMapper mapper) {
        this.listings = listings;
        this.users = users;
        this.ownerProfiles = ownerProfiles;
        this.lifecycle = lifecycle;
        this.mapper = mapper;
    }

    /** The review queue is oldest-first (fair to owners); other views are most-recently-changed first. */
    @Transactional(readOnly = true)
    public PageResponse<AdminListingSummary> list(ListingStatus status, int page) {
        if (status == ListingStatus.DRAFT) {
            throw ApiException.badRequest("INVALID_PARAMETER", "Draft listings are private to their owners.");
        }
        Sort sort = (status == ListingStatus.PENDING_VERIFICATION)
                ? Sort.by(Sort.Direction.ASC, "submittedAt")
                : Sort.by(Sort.Direction.DESC, "updatedAt");
        PageRequest pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, sort);
        Page<Listing> result = (status == null)
                ? listings.findByStatusNot(ListingStatus.DRAFT, pageable)
                : listings.findByStatus(status, pageable);
        return PageResponse.of(result, this::toSummary);
    }

    @Transactional(readOnly = true)
    public AdminListingDetail get(Long listingId) {
        return toDetail(load(listingId));
    }

    @Transactional
    public AdminListingDetail approve(Long listingId, Long adminId) {
        Listing listing = load(listingId);
        requireStatus(listing, ListingStatus.PENDING_VERIFICATION, "Only listings waiting for review can be approved.");
        lifecycle.approve(listing, admin(adminId));
        return toDetail(listing);
    }

    @Transactional
    public AdminListingDetail requestChanges(Long listingId, Long adminId, String reason) {
        Listing listing = load(listingId);
        lifecycle.transition(listing, ListingStatus.CHANGES_REQUESTED, admin(adminId), reason);
        return toDetail(listing);
    }

    @Transactional
    public AdminListingDetail reject(Long listingId, Long adminId, String reason) {
        Listing listing = load(listingId);
        lifecycle.transition(listing, ListingStatus.REJECTED, admin(adminId), reason);
        return toDetail(listing);
    }

    @Transactional
    public AdminListingDetail suspend(Long listingId, Long adminId, String reason) {
        Listing listing = load(listingId);
        lifecycle.transition(listing, ListingStatus.SUSPENDED, admin(adminId), reason);
        return toDetail(listing);
    }

    /** Reinstated listings come back as "unavailable" until the owner re-confirms their beds. */
    @Transactional
    public AdminListingDetail reinstate(Long listingId, Long adminId) {
        Listing listing = load(listingId);
        lifecycle.transition(listing, ListingStatus.PUBLISHED_UNAVAILABLE, admin(adminId),
                "Reinstated by an administrator. Please confirm your current availability.");
        return toDetail(listing);
    }

    private Listing load(Long listingId) {
        return listings.findById(listingId)
                .filter(l -> l.getStatus() != ListingStatus.DRAFT)
                .orElseThrow(() -> ApiException.notFound("LISTING_NOT_FOUND", "Listing not found."));
    }

    private static void requireStatus(Listing listing, ListingStatus expected, String message) {
        if (listing.getStatus() != expected) {
            throw ApiException.conflict("INVALID_STATUS_TRANSITION", message);
        }
    }

    private UserAccount admin(Long adminId) {
        return users.getReferenceById(adminId);
    }

    private AdminListingSummary toSummary(Listing l) {
        var min = l.minimumRent();
        return new AdminListingSummary(l.getId(), l.getTitle(), l.getLocality(), l.getStatus(),
                l.getOwner().getDisplayName(), ownerVerification(l.getOwner()),
                min.isPresent() ? min.getAsInt() : null, l.getSubmittedAt(), l.getUpdatedAt());
    }

    private AdminListingDetail toDetail(Listing l) {
        UserAccount owner = l.getOwner();
        OwnerProfile profile = ownerProfiles.findById(owner.getId()).orElse(null);
        OwnerInfo info = new OwnerInfo(owner.getId(), owner.getDisplayName(), owner.getEmail(), owner.getPhone(),
                profile == null ? null : profile.getOwnerType(),
                profile == null ? null : profile.getBusinessName(),
                profile == null ? VerificationStatus.UNVERIFIED : profile.getVerificationStatus());
        return new AdminListingDetail(mapper.toDetail(l), info);
    }

    private VerificationStatus ownerVerification(UserAccount owner) {
        return ownerProfiles.findById(owner.getId())
                .map(OwnerProfile::getVerificationStatus)
                .orElse(VerificationStatus.UNVERIFIED);
    }
}
