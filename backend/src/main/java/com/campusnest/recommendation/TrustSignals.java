package com.campusnest.recommendation;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.campusnest.engagement.ReportRepository;
import com.campusnest.engagement.ReviewRepository;

/**
 * Review and report signals for the Trust part of the suitability score (Phase 8):
 * - visible reviews only (hidden ones are ignored), combined by the scorer with a neutral prior;
 * - reports about the listing that an administrator UPHELD in the last 90 days.
 *
 * Performance (Phase 9): {@link #snapshot()} loads the signals for ALL listings in two grouped queries, so ranking
 * hundreds of listings costs 2 queries instead of 2 per listing.
 */
@Component
public class TrustSignals {

    static final Duration REPORT_WINDOW = Duration.ofDays(90);
    private static final Signals NONE = new Signals(0, 0, 0);

    private final ReviewRepository reviews;
    private final ReportRepository reports;

    public TrustSignals(ReviewRepository reviews, ReportRepository reports) {
        this.reviews = reviews;
        this.reports = reports;
    }

    public record Signals(int reviewCount, double reviewRatingSum, int upheldReportsLast90Days) {
    }

    /** Signals for every listing at once; listings without reviews or reports get zeros. */
    public Snapshot snapshot() {
        Map<Long, long[]> reviewStats = new HashMap<>();
        for (Object[] row : reviews.visibleStatsByListing()) {
            reviewStats.put((Long) row[0], new long[] {((Number) row[1]).longValue(), ((Number) row[2]).longValue()});
        }
        Map<Long, Long> upheld = new HashMap<>();
        for (Object[] row : reports.upheldListingReportsSince(Instant.now().minus(REPORT_WINDOW))) {
            upheld.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return new Snapshot(reviewStats, upheld);
    }

    public Signals forListing(Long listingId) {
        return snapshot().get(listingId);
    }

    public static final class Snapshot {
        private final Map<Long, long[]> reviewStats;
        private final Map<Long, Long> upheld;

        Snapshot(Map<Long, long[]> reviewStats, Map<Long, Long> upheld) {
            this.reviewStats = reviewStats;
            this.upheld = upheld;
        }

        public Signals get(Long listingId) {
            long[] r = reviewStats.get(listingId);
            Long u = upheld.get(listingId);
            if (r == null && u == null) {
                return NONE;
            }
            return new Signals(r == null ? 0 : (int) r[0], r == null ? 0 : r[1], u == null ? 0 : u.intValue());
        }
    }
}
