package com.campusnest.commute;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One published fare rule, always with its source and the date it applies from (see V5 migration).
 * Administrators add rules when fares change; CampusNest never invents fares.
 */
@Entity
@Table(name = "fare_rules")
public class FareRule {

    public enum Kind {
        /** base_fare for the first base_km, then per_km (auto-rickshaw meter) */
        PER_TRIP_METERED,
        /** base_fare for trips up to up_to_km (bus slabs) */
        PER_TRIP_SLAB,
        /** monthly pass price for rail distance up to up_to_km (local train) */
        MONTHLY_PASS_SLAB
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TravelMode mode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Kind kind;

    @Column(name = "up_to_km", precision = 6, scale = 2)
    private BigDecimal upToKm;

    @Column(name = "base_fare", nullable = false, precision = 8, scale = 2)
    private BigDecimal baseFare;

    @Column(name = "base_km", precision = 6, scale = 2)
    private BigDecimal baseKm;

    @Column(name = "per_km", precision = 8, scale = 2)
    private BigDecimal perKm;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(name = "source_name", nullable = false, length = 200)
    private String sourceName;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FareRule() {
    }

    public FareRule(TravelMode mode, Kind kind, BigDecimal upToKm, BigDecimal baseFare, BigDecimal baseKm, BigDecimal perKm,
            String description, String sourceName, String sourceUrl, LocalDate effectiveFrom) {
        this.mode = mode;
        this.kind = kind;
        this.upToKm = upToKm;
        this.baseFare = baseFare;
        this.baseKm = baseKm;
        this.perKm = perKm;
        this.description = description;
        this.sourceName = sourceName;
        this.sourceUrl = sourceUrl;
        this.effectiveFrom = effectiveFrom;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public TravelMode getMode() {
        return mode;
    }

    public Kind getKind() {
        return kind;
    }

    public BigDecimal getUpToKm() {
        return upToKm;
    }

    public BigDecimal getBaseFare() {
        return baseFare;
    }

    public BigDecimal getBaseKm() {
        return baseKm;
    }

    public BigDecimal getPerKm() {
        return perKm;
    }

    public String getDescription() {
        return description;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }
}
