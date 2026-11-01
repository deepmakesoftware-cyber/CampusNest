package com.campusnest.common.geo;

import java.math.BigDecimal;

/**
 * Straight-line ("as the crow flies") distance between two points on Earth, using the Haversine formula.
 * This is always available and needs no external service. It is NOT the travel distance: roads and rail
 * lines are longer, so the UI always labels it "straight-line". Real commute estimates come in Phase 6.
 */
public final class GeoDistance {

    private static final double EARTH_RADIUS_KM = 6371.0088; // mean Earth radius

    private GeoDistance() {
    }

    public static double kilometres(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLng / 2), 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
    }

    public static double kilometres(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
        return kilometres(lat1.doubleValue(), lng1.doubleValue(), lat2.doubleValue(), lng2.doubleValue());
    }

    /** Rounded to one decimal place for display, e.g. 2.4 km. */
    public static double rounded(double km) {
        return Math.round(km * 10) / 10.0;
    }
}
