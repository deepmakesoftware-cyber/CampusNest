package com.campusnest.engagement;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.campusnest.auth.AppUserPrincipal;
import com.campusnest.auth.UserAccountRepository;
import com.campusnest.common.error.ApiException;
import com.campusnest.listing.Listing;
import com.campusnest.listing.ListingRepository;
import com.campusnest.listing.ListingStatus;
import com.campusnest.listing.PublicListingService;

/**
 * A student's saved (bookmarked) listings. PUT and DELETE are idempotent: saving twice or removing something
 * not saved are both fine, which keeps the "Save" button simple.
 */
@RestController
@RequestMapping("/api/v1/student/saved")
public class SavedListingController {

    private final SavedListingRepository saved;
    private final ListingRepository listings;
    private final UserAccountRepository users;

    public SavedListingController(SavedListingRepository saved, ListingRepository listings, UserAccountRepository users) {
        this.saved = saved;
        this.listings = listings;
        this.users = users;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<SavedListingView> list(@AuthenticationPrincipal AppUserPrincipal me) {
        return saved.findByStudentIdOrderByCreatedAtDesc(me.getId()).stream().map(s -> {
            Listing l = s.getListing();
            boolean visible = PublicListingService.isVisible(l);
            // If a saved listing is later suspended or sent back to review, keep the bookmark but say so.
            return new SavedListingView(l.getId(), l.getTitle(), l.getLocality(), visible,
                    visible ? l.getStatus() : null, s.getCreatedAt());
        }).toList();
    }

    @PutMapping("/{listingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void save(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long listingId) {
        if (saved.findByStudentIdAndListingId(me.getId(), listingId).isPresent()) {
            return;
        }
        Listing listing = listings.findById(listingId)
                .filter(PublicListingService::isVisible)
                .orElseThrow(() -> ApiException.notFound("LISTING_NOT_FOUND", "Listing not found."));
        saved.save(new SavedListing(users.getReferenceById(me.getId()), listing));
    }

    @DeleteMapping("/{listingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void unsave(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long listingId) {
        saved.findByStudentIdAndListingId(me.getId(), listingId).ifPresent(saved::delete);
    }

    /** status is null when the listing is no longer visible to students. */
    public record SavedListingView(Long listingId, String title, String locality, boolean available,
            ListingStatus status, Instant savedAt) {
    }
}
