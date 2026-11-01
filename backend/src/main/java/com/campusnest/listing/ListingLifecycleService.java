package com.campusnest.listing;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.campusnest.auth.UserAccount;

/**
 * The ONLY place where a listing's status changes. It checks the move against the state machine
 * (ListingLifecycle) and writes an audit row for every change. Callers handle permissions and
 * transactions; this class handles consistency.
 */
@Service
public class ListingLifecycleService {

    private final ListingStatusChangeRepository history;

    public ListingLifecycleService(ListingStatusChangeRepository history) {
        this.history = history;
    }

    /** Records the creation of a new draft (no "from" status). */
    void recordCreated(Listing listing, UserAccount actor) {
        history.save(new ListingStatusChange(listing, null, ListingStatus.DRAFT, actor, null, Instant.now()));
    }

    /**
     * Moves the listing to a new status.
     *
     * @param reason shown to the owner for admin decisions (required for changes requested / reject / suspend,
     *               enforced by the request DTO); may be null otherwise
     */
    public void transition(Listing listing, ListingStatus to, UserAccount actor, String reason) {
        ListingStatus from = listing.getStatus();
        ListingLifecycle.requireTransition(from, to);
        Instant now = Instant.now();
        String trimmed = (reason == null || reason.isBlank()) ? null : reason.trim();
        listing.applyStatus(to, trimmed, now);
        history.save(new ListingStatusChange(listing, from, to, actor, trimmed, now));
    }

    /** Admin approval: published as available if any bed is free, otherwise as unavailable. */
    public void approve(Listing listing, UserAccount admin) {
        ListingStatus target = listing.totalBedsAvailable() > 0
                ? ListingStatus.PUBLISHED_AVAILABLE
                : ListingStatus.PUBLISHED_UNAVAILABLE;
        transition(listing, target, admin, null);
    }
}
