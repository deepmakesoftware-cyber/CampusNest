package com.campusnest.commute;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A suburban railway station (reference data from OpenStreetMap, see V5 migration). Read-only. */
@Entity
@Table(name = "stations")
public class Station {

    @Id
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String line;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "osm_node_id", nullable = false)
    private Long osmNodeId;

    protected Station() {
    }

    /** For unit tests. */
    Station(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = BigDecimal.valueOf(latitude);
        this.longitude = BigDecimal.valueOf(longitude);
    }

    public String getName() {
        return name;
    }

    public double lat() {
        return latitude.doubleValue();
    }

    public double lng() {
        return longitude.doubleValue();
    }
}
