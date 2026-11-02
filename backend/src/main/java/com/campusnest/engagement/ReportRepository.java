package com.campusnest.engagement;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findByStatus(Report.Status status, Pageable pageable);

    boolean existsByReporterIdAndTargetTypeAndTargetIdAndStatus(Long reporterId, Report.TargetType type, Long targetId,
            Report.Status status);

    long countByReporterIdAndCreatedAtAfter(Long reporterId, Instant since);

    long countByTargetTypeAndTargetIdAndStatusAndResolvedAtAfter(Report.TargetType type, Long targetId,
            Report.Status status, Instant since);

    long countByStatus(Report.Status status);
}
