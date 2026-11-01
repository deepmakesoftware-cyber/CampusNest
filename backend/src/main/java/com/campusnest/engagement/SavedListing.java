package com.campusnest.engagement;

import java.time.Instant;

import com.campusnest.auth.UserAccount;
import com.campusnest.listing.Listing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A listing a student bookmarked to compare or come back to later. */
@Entity
@Table(name = "saved_listings")
public class SavedListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private UserAccount student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id")
    private Listing listing;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SavedListing() {
    }

    SavedListing(UserAccount student, Listing listing) {
        this.student = student;
        this.listing = listing;
        this.createdAt = Instant.now();
    }

    public Listing getListing() {
        return listing;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
