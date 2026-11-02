package com.campusnest.engagement;

import java.time.Duration;
import java.time.Instant;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusnest.auth.Role;
import com.campusnest.auth.UserAccount;
import com.campusnest.auth.UserAccountRepository;
import com.campusnest.common.error.ApiException;
import com.campusnest.common.web.PageResponse;
import com.campusnest.listing.ListingRepository;
import com.campusnest.listing.PublicListingService;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Reports (FR-3): any logged-in student or owner can report a published listing, a visible review, or another user.
 * Administrators uphold or dismiss them with a note. Upheld listing reports lower that listing's trust score for
 * 90 days; further action (suspending a listing or account) is a separate, deliberate admin decision.
 */
@Service
public class ReportService {

    static final int MAX_REPORTS_PER_DAY = 10;
    static final int PAGE_SIZE = 20;

    private final ReportRepository reports;
    private final ListingRepository listings;
    private final ReviewRepository reviews;
    private final UserAccountRepository users;

    public ReportService(ReportRepository reports, ListingRepository listings, ReviewRepository reviews,
            UserAccountRepository users) {
        this.reports = reports;
        this.listings = listings;
        this.reviews = reviews;
        this.users = users;
    }

    @Transactional
    public ReportView create(Long reporterId, CreateReport r) {
        switch (r.targetType()) {
            case LISTING -> listings.findById(r.targetId()).filter(PublicListingService::isVisible)
                    .orElseThrow(() -> ApiException.notFound("TARGET_NOT_FOUND", "Listing not found."));
            case REVIEW -> reviews.findById(r.targetId()).filter(Review::isVisible)
                    .orElseThrow(() -> ApiException.notFound("TARGET_NOT_FOUND", "Review not found."));
            case USER -> {
                UserAccount target = users.findById(r.targetId()).filter(u -> u.getRole() != Role.ADMIN)
                        .orElseThrow(() -> ApiException.notFound("TARGET_NOT_FOUND", "User not found."));
                if (target.getId().equals(reporterId)) {
                    throw ApiException.badRequest("INVALID_REPORT", "You cannot report yourself.");
                }
            }
        }
        if (reports.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(reporterId, r.targetType(), r.targetId(), Report.Status.OPEN)) {
            throw ApiException.conflict("ALREADY_REPORTED", "You have already reported this; an administrator will look at it.");
        }
        if (reports.countByReporterIdAndCreatedAtAfter(reporterId, Instant.now().minus(Duration.ofHours(24))) >= MAX_REPORTS_PER_DAY) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REPORTS", "You have sent many reports today. Please try again tomorrow.");
        }
        Report saved = reports.save(new Report(users.getReferenceById(reporterId), r.targetType(), r.targetId(), r.reason(), r.details()));
        return toView(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportView> list(Report.Status status, int page) {
        var pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE,
                Sort.by(status == Report.Status.OPEN ? Sort.Direction.ASC : Sort.Direction.DESC, "createdAt"));
        var result = status == null ? reports.findAll(pageable) : reports.findByStatus(status, pageable);
        return PageResponse.of(result, this::toView);
    }

    @Transactional
    public ReportView resolve(Long reportId, Long adminId, ResolveReport r) {
        Report report = reports.findById(reportId)
                .orElseThrow(() -> ApiException.notFound("REPORT_NOT_FOUND", "Report not found."));
        if (report.getStatus() != Report.Status.OPEN) {
            throw ApiException.conflict("REPORT_CLOSED", "This report has already been resolved.");
        }
        if (r.outcome() == Report.Status.OPEN) {
            throw ApiException.badRequest("INVALID_OUTCOME", "Choose UPHELD or DISMISSED.");
        }
        report.resolve(r.outcome(), users.getReferenceById(adminId), r.note());
        return toView(report);
    }

    /** Admin view, with a short description of what was reported so the queue is readable. */
    private ReportView toView(Report r) {
        String summary = switch (r.getTargetType()) {
            case LISTING -> listings.findById(r.getTargetId()).map(l -> "Listing: " + l.getTitle()).orElse("Listing (deleted)");
            case REVIEW -> reviews.findById(r.getTargetId())
                    .map(v -> "Review of " + v.getListing().getTitle() + ": " + v.getRating() + "★ "
                            + (v.getComment() == null ? "" : "“" + abbreviate(v.getComment()) + "”"))
                    .orElse("Review (deleted)");
            case USER -> users.findById(r.getTargetId())
                    .map(u -> "User: " + u.getDisplayName() + " (" + u.getRole().name().toLowerCase() + ")").orElse("User (deleted)");
        };
        return new ReportView(r.getId(), r.getTargetType(), r.getTargetId(), summary, r.getReason(), r.getDetails(),
                r.getReporter().getDisplayName(), r.getStatus(), r.getResolutionNote(), r.getCreatedAt(), r.getResolvedAt());
    }

    private static String abbreviate(String s) {
        return s.length() > 80 ? s.substring(0, 80) + "…" : s;
    }

    // ---------------- DTOs ----------------

    public record CreateReport(@NotNull Report.TargetType targetType, @NotNull Long targetId, @NotNull Report.Reason reason,
            @Size(max = 1000) String details) {
    }

    public record ResolveReport(@NotNull Report.Status outcome, @NotBlank @Size(max = 1000) String note) {
    }

    public record ReportView(Long id, Report.TargetType targetType, Long targetId, String targetSummary, Report.Reason reason,
            String details, String reporterName, Report.Status status, String resolutionNote, Instant createdAt,
            Instant resolvedAt) {
    }
}
