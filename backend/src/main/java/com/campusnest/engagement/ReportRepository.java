package com.campusnest.engagement;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findByStatus(Report.Status status, Pageable pageable);

    boolean existsByReporterIdAndTargetTypeAndTargetIdAndStatus(Long reporterId, Report.TargetType type, Long targetId,
            Report.Status status);

    long countByReporterIdAndCreatedAtAfter(Long reporterId, Instant since);

    long countByTargetTypeAndTargetIdAndStatusAndResolvedAtAfter(Report.TargetType type, Long targetId,
            Report.Status status, Instant since);

    long countByStatus(Report.Status status);

    /** [listingId, count] of listing reports upheld since the given time: one query for a whole ranking. */
    @Query("""
            select r.targetId, count(r) from Report r
            where r.targetType = com.campusnest.engagement.Report.TargetType.LISTING
              and r.status = com.campusnest.engagement.Report.Status.UPHELD and r.resolvedAt > :since
            group by r.targetId
            """)
    List<Object[]> upheldListingReportsSince(Instant since);
}
