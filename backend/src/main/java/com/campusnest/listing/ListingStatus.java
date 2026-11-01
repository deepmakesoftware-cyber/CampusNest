package com.campusnest.listing;

/**
 * Where a listing is in its life. Allowed moves between these states are defined in {@link ListingLifecycle}.
 * Only the two PUBLISHED_* states are ever visible to students.
 */
public enum ListingStatus {
    DRAFT,
    PENDING_VERIFICATION,
    CHANGES_REQUESTED,
    PUBLISHED_AVAILABLE,
    PUBLISHED_UNAVAILABLE,
    SUSPENDED,
    REJECTED;

    public boolean isPublished() {
        return this == PUBLISHED_AVAILABLE || this == PUBLISHED_UNAVAILABLE;
    }
}
