package com.campusnest.listing;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Metadata for one listing photo. The bytes are stored on disk by ImageStorage under storageKey. */
@Entity
@Table(name = "listing_images")
public class ListingImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id")
    private Listing listing;

    @Column(name = "storage_key", nullable = false, length = 100)
    private String storageKey;

    @Column(name = "content_type", nullable = false, length = 30)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private int sizeBytes;

    @Column(nullable = false)
    private int width;

    @Column(nullable = false)
    private int height;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    protected ListingImage() {
    }

    ListingImage(Listing listing, String storageKey, String contentType, int sizeBytes, int width, int height) {
        this.listing = listing;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.width = width;
        this.height = height;
        this.uploadedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Listing getListing() {
        return listing;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getContentType() {
        return contentType;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
