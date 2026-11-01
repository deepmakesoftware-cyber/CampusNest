package com.campusnest.listing;

import java.time.Instant;

import com.campusnest.listing.ListingTypes.RoomType;

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

/** One room type offered by a listing, e.g. "Double sharing, ₹9,000/month, 2 of 6 beds free". */
@Entity
@Table(name = "room_options")
public class RoomOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id")
    private Listing listing;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false, length = 20)
    private RoomType roomType;

    @Column(name = "sharing_capacity", nullable = false)
    private int sharingCapacity;

    @Column(name = "monthly_rent", nullable = false)
    private int monthlyRent;

    @Column(name = "security_deposit")
    private Integer securityDeposit;

    @Column(name = "beds_total", nullable = false)
    private int bedsTotal;

    @Column(name = "beds_available", nullable = false)
    private int bedsAvailable;

    @Column(name = "availability_updated_at")
    private Instant availabilityUpdatedAt;

    protected RoomOption() {
    }

    RoomOption(Listing listing) {
        this.listing = listing;
    }

    /** Updates all fields; records when the bed count last changed. */
    void update(RoomType roomType, int sharingCapacity, int monthlyRent, Integer securityDeposit,
            int bedsTotal, int bedsAvailable, Instant now) {
        if (this.id == null || this.bedsAvailable != bedsAvailable || this.availabilityUpdatedAt == null) {
            this.availabilityUpdatedAt = now;
        }
        this.roomType = roomType;
        this.sharingCapacity = sharingCapacity;
        this.monthlyRent = monthlyRent;
        this.securityDeposit = securityDeposit;
        this.bedsTotal = bedsTotal;
        this.bedsAvailable = bedsAvailable;
    }

    void updateAvailability(int bedsAvailable, Instant now) {
        this.bedsAvailable = bedsAvailable;
        this.availabilityUpdatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public int getSharingCapacity() {
        return sharingCapacity;
    }

    public int getMonthlyRent() {
        return monthlyRent;
    }

    public Integer getSecurityDeposit() {
        return securityDeposit;
    }

    public int getBedsTotal() {
        return bedsTotal;
    }

    public int getBedsAvailable() {
        return bedsAvailable;
    }

    public Instant getAvailabilityUpdatedAt() {
        return availabilityUpdatedAt;
    }
}
