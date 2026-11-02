package com.campusnest.owner;

import java.time.Instant;

import com.campusnest.auth.UserAccount;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** Owner-only details. Shares its primary key with the user account (one-to-one). */
@Entity
@Table(name = "owner_profiles")
public class OwnerProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 20)
    private OwnerType ownerType;

    @Column(name = "business_name", length = 120)
    private String businessName;

    // No public setter: owners cannot verify themselves. Changed only through admin actions.
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private VerificationStatus verificationStatus;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private UserAccount verifiedBy;

    @Column(name = "verification_requested_at")
    private Instant verificationRequestedAt;

    protected OwnerProfile() {
    }

    public OwnerProfile(UserAccount user) {
        this.user = user;
        this.ownerType = OwnerType.INDIVIDUAL;
        this.verificationStatus = VerificationStatus.UNVERIFIED;
    }

    public void update(OwnerType ownerType, String businessName) {
        this.ownerType = ownerType;
        this.businessName = (businessName == null || businessName.isBlank()) ? null : businessName.trim();
    }

    /** Recorded when an administrator has checked the owner's identity. */
    public void markVerified(UserAccount admin, Instant when) {
        this.verificationStatus = VerificationStatus.VERIFIED;
        this.verifiedBy = admin;
        this.verifiedAt = when;
    }

    /** The owner asks CampusNest to check their identity. */
    public void requestVerification(Instant when) {
        this.verificationStatus = VerificationStatus.PENDING;
        this.verificationRequestedAt = when;
    }

    public void rejectVerification() {
        this.verificationStatus = VerificationStatus.REJECTED;
        this.verifiedBy = null;
        this.verifiedAt = null;
    }

    /** Withdraws a previous verification (e.g. after upheld fraud reports). The owner may ask again later. */
    public void revokeVerification() {
        this.verificationStatus = VerificationStatus.UNVERIFIED;
        this.verifiedBy = null;
        this.verifiedAt = null;
    }

    public Instant getVerificationRequestedAt() {
        return verificationRequestedAt;
    }

    public UserAccount getUser() {
        return user;
    }

    public Long getUserId() {
        return userId;
    }

    public OwnerType getOwnerType() {
        return ownerType;
    }

    public String getBusinessName() {
        return businessName;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }
}
