package com.campusnest.listing;

import static com.campusnest.listing.ListingStatus.CHANGES_REQUESTED;
import static com.campusnest.listing.ListingStatus.DRAFT;
import static com.campusnest.listing.ListingStatus.PENDING_VERIFICATION;
import static com.campusnest.listing.ListingStatus.PUBLISHED_AVAILABLE;
import static com.campusnest.listing.ListingStatus.PUBLISHED_UNAVAILABLE;
import static com.campusnest.listing.ListingStatus.REJECTED;
import static com.campusnest.listing.ListingStatus.SUSPENDED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.campusnest.common.error.ApiException;

/** Unit tests for the listing state machine table (no Spring, no database). */
class ListingLifecycleTest {

    @ParameterizedTest(name = "{0} -> {1} is allowed")
    @CsvSource({
            "DRAFT, PENDING_VERIFICATION",
            "PENDING_VERIFICATION, PUBLISHED_AVAILABLE",
            "PENDING_VERIFICATION, PUBLISHED_UNAVAILABLE",
            "PENDING_VERIFICATION, CHANGES_REQUESTED",
            "PENDING_VERIFICATION, REJECTED",
            "CHANGES_REQUESTED, PENDING_VERIFICATION",
            "PUBLISHED_AVAILABLE, PUBLISHED_UNAVAILABLE",
            "PUBLISHED_UNAVAILABLE, PUBLISHED_AVAILABLE",
            "PUBLISHED_AVAILABLE, PENDING_VERIFICATION",
            "PUBLISHED_AVAILABLE, SUSPENDED",
            "PUBLISHED_UNAVAILABLE, SUSPENDED",
            "SUSPENDED, PUBLISHED_UNAVAILABLE"
    })
    void allowedTransitions(ListingStatus from, ListingStatus to) {
        assertThat(ListingLifecycle.canTransition(from, to)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1} is forbidden")
    @CsvSource({
            "DRAFT, PUBLISHED_AVAILABLE",          // cannot skip review
            "DRAFT, SUSPENDED",
            "CHANGES_REQUESTED, PUBLISHED_AVAILABLE",
            "PENDING_VERIFICATION, SUSPENDED",
            "SUSPENDED, PUBLISHED_AVAILABLE",      // owner must re-confirm availability first
            "REJECTED, PENDING_VERIFICATION",      // rejection is final
            "REJECTED, PUBLISHED_AVAILABLE",
            "PUBLISHED_AVAILABLE, DRAFT"
    })
    void forbiddenTransitions(ListingStatus from, ListingStatus to) {
        assertThat(ListingLifecycle.canTransition(from, to)).isFalse();
    }

    @Test
    void forbiddenTransitionThrowsConflict() {
        assertThatThrownBy(() -> ListingLifecycle.requireTransition(DRAFT, PUBLISHED_AVAILABLE))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("draft")
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo("INVALID_STATUS_TRANSITION");
    }

    @Test
    void rejectedIsTerminal() {
        for (ListingStatus to : ListingStatus.values()) {
            assertThat(ListingLifecycle.canTransition(REJECTED, to)).isFalse();
        }
    }

    @Test
    void ownerCanEditOnlyOutsideReviewAndModeration() {
        assertThat(ListingLifecycle.isOwnerEditable(DRAFT)).isTrue();
        assertThat(ListingLifecycle.isOwnerEditable(CHANGES_REQUESTED)).isTrue();
        assertThat(ListingLifecycle.isOwnerEditable(PUBLISHED_AVAILABLE)).isTrue();
        assertThat(ListingLifecycle.isOwnerEditable(PUBLISHED_UNAVAILABLE)).isTrue();
        assertThat(ListingLifecycle.isOwnerEditable(PENDING_VERIFICATION)).isFalse();
        assertThat(ListingLifecycle.isOwnerEditable(SUSPENDED)).isFalse();
        assertThat(ListingLifecycle.isOwnerEditable(REJECTED)).isFalse();
    }
}
