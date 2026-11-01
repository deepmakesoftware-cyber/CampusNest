package com.campusnest.commute;

import com.campusnest.common.geo.GeoDistance;

/**
 * Commute estimate from straight-line distance only. Always available, needs no external service,
 * and is always marked approximate. Phase 6 adds better estimates (rail stations, road routes) and falls
 * back to this one when they are not available.
 *
 * ASSUMPTIONS (documented in docs/ALGORITHMS.md; to be validated with students):
 * - Roads are about 30% longer than the straight line (DETOUR_FACTOR = 1.3), a common rule of thumb for
 *   dense city street networks.
 * - Walking speed 4.5 km/h, a typical planning value for adults.
 * - Auto-rickshaw average 18 km/h including Mumbai traffic, plus 5 minutes to find one. This is an assumption,
 *   not measured data.
 * - Walking is chosen automatically ("ANY") for trips up to 1.5 km.
 */
public final class ApproximateCommute {

    static final double DETOUR_FACTOR = 1.3;
    static final double WALK_KMH = 4.5;
    static final double AUTO_KMH = 18.0;
    static final int AUTO_WAIT_MINUTES = 5;
    static final double WALKABLE_KM = 1.5;

    private ApproximateCommute() {
    }

    public static CommuteEstimate estimate(double fromLat, double fromLng, double toLat, double toLng, TravelMode preferred) {
        double straight = GeoDistance.kilometres(fromLat, fromLng, toLat, toLng);
        double road = straight * DETOUR_FACTOR;
        TravelMode mode = switch (preferred) {
            case WALK -> TravelMode.WALK;
            case AUTO_RICKSHAW -> TravelMode.AUTO_RICKSHAW;
            // Bus and train need timetable/station data (Phase 6); until then use the practical road option.
            case ANY, BUS, LOCAL_TRAIN -> road <= WALKABLE_KM ? TravelMode.WALK : TravelMode.AUTO_RICKSHAW;
        };
        int minutes = mode == TravelMode.WALK
                ? (int) Math.ceil(road / WALK_KMH * 60)
                : (int) Math.ceil(road / AUTO_KMH * 60) + AUTO_WAIT_MINUTES;
        String how = "Estimated from straight-line distance (" + GeoDistance.rounded(straight) + " km) × "
                + DETOUR_FACTOR + " at an assumed " + (mode == TravelMode.WALK ? WALK_KMH + " km/h walking speed"
                : AUTO_KMH + " km/h auto-rickshaw speed plus " + AUTO_WAIT_MINUTES + " min waiting");
        return new CommuteEstimate(mode, GeoDistance.rounded(road), Math.max(minutes, 1), true, how);
    }
}
