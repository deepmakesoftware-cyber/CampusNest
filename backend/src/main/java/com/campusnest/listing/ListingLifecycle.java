package com.campusnest.listing;

import static com.campusnest.listing.ListingStatus.CHANGES_REQUESTED;
import static com.campusnest.listing.ListingStatus.DRAFT;
import static com.campusnest.listing.ListingStatus.PENDING_VERIFICATION;
import static com.campusnest.listing.ListingStatus.PUBLISHED_AVAILABLE;
import static com.campusnest.listing.ListingStatus.PUBLISHED_UNAVAILABLE;
import static com.campusnest.listing.ListingStatus.REJECTED;
import static com.campusnest.listing.ListingStatus.SUSPENDED;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.campusnest.common.error.ApiException;

/**
 * The listing state machine (FR-3), written as a plain table so it is easy to read, test and explain.
 * See the diagram in docs/ALGORITHMS.md section 3. Every status change in the application goes through
 * {@link #requireTransition}, so an illegal move (e.g. DRAFT straight to PUBLISHED) is impossible.
 */
public final class ListingLifecycle {

    private static final Map<ListingStatus, Set<ListingStatus>> ALLOWED = new EnumMap<>(ListingStatus.class);

    static {
        // owner submits
        ALLOWED.put(DRAFT, EnumSet.of(PENDING_VERIFICATION));
        // admin decides
        ALLOWED.put(PENDING_VERIFICATION, EnumSet.of(PUBLISHED_AVAILABLE, PUBLISHED_UNAVAILABLE, CHANGES_REQUESTED, REJECTED));
        // owner fixes and resubmits
        ALLOWED.put(CHANGES_REQUESTED, EnumSet.of(PENDING_VERIFICATION));
        // owner updates beds; owner makes a material edit (back to review); admin suspends
        ALLOWED.put(PUBLISHED_AVAILABLE, EnumSet.of(PUBLISHED_UNAVAILABLE, PENDING_VERIFICATION, SUSPENDED));
        ALLOWED.put(PUBLISHED_UNAVAILABLE, EnumSet.of(PUBLISHED_AVAILABLE, PENDING_VERIFICATION, SUSPENDED));
        // admin reinstates; the owner must then re-confirm availability
        ALLOWED.put(SUSPENDED, EnumSet.of(PUBLISHED_UNAVAILABLE));
        // final
        ALLOWED.put(REJECTED, EnumSet.noneOf(ListingStatus.class));
    }

    private ListingLifecycle() {
    }

    public static boolean canTransition(ListingStatus from, ListingStatus to) {
        return ALLOWED.get(from).contains(to);
    }

    /** Throws 409 Conflict if the move is not in the table. */
    public static void requireTransition(ListingStatus from, ListingStatus to) {
        if (!canTransition(from, to)) {
            throw ApiException.conflict("INVALID_STATUS_TRANSITION",
                    "A listing that is " + label(from) + " cannot become " + label(to) + ".");
        }
    }

    /** Can the owner change the listing's content in this state? (Locked while under review or after rejection/suspension.) */
    public static boolean isOwnerEditable(ListingStatus status) {
        return status == DRAFT || status == CHANGES_REQUESTED || status.isPublished();
    }

    private static String label(ListingStatus status) {
        return status.name().toLowerCase().replace('_', ' ');
    }
}
