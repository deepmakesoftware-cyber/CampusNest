package com.campusnest.listing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Reference data seeded by V2__listings.sql (Wi-Fi, meals, AC, ...). */
@Entity
@Table(name = "amenities")
public class Amenity {

    @Id
    private Long id;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    /** Core amenities must be answered yes/no before a listing can be submitted. */
    @Column(name = "is_core", nullable = false)
    private boolean core;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected Amenity() {
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public boolean isCore() {
        return core;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
