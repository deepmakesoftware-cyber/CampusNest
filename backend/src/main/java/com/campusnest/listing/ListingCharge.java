package com.campusnest.listing;

import com.campusnest.listing.ListingTypes.ChargeFrequency;
import com.campusnest.listing.ListingTypes.ChargeType;

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

/** An extra cost on top of rent, as disclosed by the owner (e.g. electricity ₹800/month, mandatory). */
@Entity
@Table(name = "listing_charges")
public class ListingCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id")
    private Listing listing;

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_type", nullable = false, length = 20)
    private ChargeType chargeType;

    @Column(length = 100)
    private String description;

    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChargeFrequency frequency;

    @Column(nullable = false)
    private boolean mandatory;

    protected ListingCharge() {
    }

    ListingCharge(Listing listing, ChargeType chargeType, String description, int amount,
            ChargeFrequency frequency, boolean mandatory) {
        this.listing = listing;
        this.chargeType = chargeType;
        this.description = (description == null || description.isBlank()) ? null : description.trim();
        this.amount = amount;
        this.frequency = frequency;
        this.mandatory = mandatory;
    }

    public ChargeType getChargeType() {
        return chargeType;
    }

    public String getDescription() {
        return description;
    }

    public int getAmount() {
        return amount;
    }

    public ChargeFrequency getFrequency() {
        return frequency;
    }

    public boolean isMandatory() {
        return mandatory;
    }
}
