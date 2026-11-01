package com.campusnest.recommendation;

import org.springframework.stereotype.Component;

/**
 * Review and report signals for the Trust part of the score.
 * Until reviews and reports exist (Phase 8) every listing has none: the scorer then uses its neutral rating prior,
 * so no listing is rewarded or punished for missing data.
 */
@Component
public class TrustSignals {

    public record Signals(int reviewCount, double reviewRatingSum, int upheldReportsLast90Days) {
    }

    public Signals forListing(Long listingId) {
        return new Signals(0, 0, 0);
    }
}
