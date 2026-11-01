package com.campusnest.listing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * The owner's yes/no answer for one amenity. If there is no row for an amenity, the owner has not said
 * (shown as "not specified"); that is different from available = false ("confirmed not available").
 */
@Entity
@Table(name = "listing_amenities")
public class ListingAmenity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id")
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "amenity_id")
    private Amenity amenity;

    @Column(nullable = false)
    private boolean available;

    protected ListingAmenity() {
    }

    ListingAmenity(Listing listing, Amenity amenity, boolean available) {
        this.listing = listing;
        this.amenity = amenity;
        this.available = available;
    }

    void setAvailable(boolean available) {
        this.available = available;
    }

    public Amenity getAmenity() {
        return amenity;
    }

    public boolean isAvailable() {
        return available;
    }
}
