package com.campusnest.listing;

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

/** Audit trail row (table listing_status_history): who moved a listing from one status to another, and why. */
@Entity
@Table(name = "listing_status_history")
public class ListingStatusChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id")
    private Listing listing;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private ListingStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private ListingStatus toStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private UserAccount actor;

    @Column(length = 1000)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ListingStatusChange() {
    }

    ListingStatusChange(Listing listing, ListingStatus fromStatus, ListingStatus toStatus, UserAccount actor,
            String reason, Instant createdAt) {
        this.listing = listing;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actor = actor;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public ListingStatus getFromStatus() {
        return fromStatus;
    }

    public ListingStatus getToStatus() {
        return toStatus;
    }

    public UserAccount getActor() {
        return actor;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
