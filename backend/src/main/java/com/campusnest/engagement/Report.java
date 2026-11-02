package com.campusnest.engagement;

import java.time.Instant;

import com.campusnest.auth.UserAccount;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A user's report about a listing, a review or another user, handled by administrators. */
@Entity
@Table(name = "reports")
public class Report {

    public enum TargetType {
        LISTING,
        REVIEW,
        USER
    }

    public enum Reason {
        FAKE_LISTING,
        WRONG_INFORMATION,
        UNSAFE,
        SPAM,
        HARASSMENT,
        INAPPROPRIATE,
        OTHER
    }

    public enum Status {
        OPEN,
        /** the administrator agreed with the report (counts against a listing's trust score for 90 days) */
        UPHELD,
        DISMISSED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id")
    private UserAccount reporter;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Reason reason;

    @Column(length = 1000)
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private UserAccount resolvedBy;

    @Column(name = "resolution_note", length = 1000)
    private String resolutionNote;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected Report() {
    }

    Report(UserAccount reporter, TargetType targetType, Long targetId, Reason reason, String details) {
        this.reporter = reporter;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        this.details = (details == null || details.isBlank()) ? null : details.trim();
        this.status = Status.OPEN;
        this.createdAt = Instant.now();
    }

    void resolve(Status outcome, UserAccount admin, String note) {
        this.status = outcome;
        this.resolvedBy = admin;
        this.resolutionNote = note.trim();
        this.resolvedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UserAccount getReporter() {
        return reporter;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public Reason getReason() {
        return reason;
    }

    public String getDetails() {
        return details;
    }

    public Status getStatus() {
        return status;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}
