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

/** A student's rating (1–5) and optional comment about a listing. Hidden reviews are kept but not shown or counted. */
@Entity
@Table(name = "reviews")
public class Review {

    public static final String VISIBLE = "VISIBLE";
    public static final String HIDDEN = "HIDDEN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id")
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private UserAccount student;

    @Column(nullable = false)
    private int rating;

    @Column(length = 1000)
    private String comment;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "hidden_reason", length = 500)
    private String hiddenReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Review() {
    }

    Review(Listing listing, UserAccount student) {
        this.listing = listing;
        this.student = student;
        this.status = VISIBLE;
        this.createdAt = Instant.now();
    }

    void edit(int rating, String comment) {
        this.rating = rating;
        this.comment = (comment == null || comment.isBlank()) ? null : comment.trim();
        this.updatedAt = Instant.now();
    }

    void hide(String reason) {
        this.status = HIDDEN;
        this.hiddenReason = reason.trim();
    }

    void unhide() {
        this.status = VISIBLE;
        this.hiddenReason = null;
    }

    public boolean isVisible() {
        return VISIBLE.equals(status);
    }

    public Long getId() {
        return id;
    }

    public Listing getListing() {
        return listing;
    }

    public UserAccount getStudent() {
        return student;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public String getStatus() {
        return status;
    }

    public String getHiddenReason() {
        return hiddenReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
