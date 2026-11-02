package com.campusnest.moderation;

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

/**
 * Audit record of an owner-verification decision: who decided, how they checked, and why.
 * No identity documents are stored (decision D4); "DOCUMENT_SEEN_NOT_STORED" means the admin looked at one.
 */
@Entity
@Table(name = "verification_records")
public class VerificationRecord {

    public enum Decision {
        VERIFIED,
        REJECTED,
        REVOKED
    }

    public enum Method {
        PHONE_CALL,
        VIDEO_CALL,
        IN_PERSON_VISIT,
        DOCUMENT_SEEN_NOT_STORED,
        OTHER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private UserAccount owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Decision decision;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Method method;

    @Column(nullable = false, length = 1000)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_id")
    private UserAccount reviewer;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected VerificationRecord() {
    }

    VerificationRecord(UserAccount owner, Decision decision, Method method, String notes, UserAccount reviewer) {
        this.owner = owner;
        this.decision = decision;
        this.method = method;
        this.notes = notes.trim();
        this.reviewer = reviewer;
        this.createdAt = Instant.now();
    }

    public Decision getDecision() {
        return decision;
    }

    public Method getMethod() {
        return method;
    }

    public String getNotes() {
        return notes;
    }

    public UserAccount getReviewer() {
        return reviewer;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
