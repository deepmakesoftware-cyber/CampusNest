package com.campusnest.recommendation;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;

import com.campusnest.engagement.Report;
import com.campusnest.engagement.ReportRepository;
import com.campusnest.engagement.ReviewRepository;

/**
 * Review and report signals for the Trust part of the suitability score (Phase 8):
 * - visible reviews only (hidden ones are ignored), combined by the scorer with a neutral prior;
 * - reports about the listing that an administrator UPHELD in the last 90 days.
 */
@Component
public class TrustSignals {

    static final Duration REPORT_WINDOW = Duration.ofDays(90);

    private final ReviewRepository reviews;
    private final ReportRepository reports;

    public TrustSignals(ReviewRepository reviews, ReportRepository reports) {
        this.reviews = reviews;
        this.reports = reports;
    }

    public record Signals(int reviewCount, double reviewRatingSum, int upheldReportsLast90Days) {
    }

    public Signals forListing(Long listingId) {
        List<Object[]> stats = reviews.visibleStats(listingId);
        Object[] row = stats.isEmpty() ? new Object[] {0L, 0L} : stats.get(0);
        int count = ((Number) row[0]).intValue();
        double sum = ((Number) row[1]).doubleValue();
        long upheld = reports.countByTargetTypeAndTargetIdAndStatusAndResolvedAtAfter(Report.TargetType.LISTING, listingId,
                Report.Status.UPHELD, Instant.now().minus(REPORT_WINDOW));
        return new Signals(count, sum, (int) upheld);
    }
}
