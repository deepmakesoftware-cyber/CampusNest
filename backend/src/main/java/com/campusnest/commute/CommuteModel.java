package com.campusnest.commute;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.campusnest.common.geo.GeoDistance;

/**
 * How CampusNest ESTIMATES travel time for each mode (Phase 6). Every number below is a documented assumption
 * (docs/ALGORITHMS.md, section 4), not measured data, and every result is labelled approximate.
 *
 * - WALK, AUTO_RICKSHAW: road distance = straight line × 1.3 (see ApproximateCommute).
 * - BUS: same road distance at 13 km/h average plus 10 minutes waiting. Assumes a direct BEST route exists,
 *   which CampusNest cannot check (no route data), so the UI tells the student to confirm the route.
 * - LOCAL_TRAIN: walk to the station nearest the listing, wait, ride, walk from the station nearest the college.
 *   Ride distance = straight line between the stations × 1.2 at 30 km/h average including stops.
 *   Not offered when both ends use the same station, the rail part would be under 2 km (walking or an auto is
 *   then always better), or the walk to the station would take over 25 minutes.
 */
public final class CommuteModel {

    static final double BUS_KMH = 13.0;
    static final int BUS_WAIT_MINUTES = 10;
    static final double RAIL_DETOUR = 1.2;
    static final double TRAIN_KMH = 30.0;
    static final int TRAIN_WAIT_MINUTES = 8;
    static final int MAX_WALK_TO_STATION_MINUTES = 25;
    static final double MIN_RAIL_KM = 2.0;
    static final int MAX_PRACTICAL_WALK_MINUTES = 30;

    private CommuteModel() {
    }

    /** The journey by local train, broken into legs so it can be explained. */
    public record TrainJourney(String fromStation, String toStation, int walkToStationMinutes, int waitMinutes,
            int rideMinutes, int walkFromStationMinutes, double railKm) {

        public int totalMinutes() {
            return walkToStationMinutes + waitMinutes + rideMinutes + walkFromStationMinutes;
        }
    }

    static int walkMinutes(double straightKm) {
        return (int) Math.ceil(straightKm * ApproximateCommute.DETOUR_FACTOR / ApproximateCommute.WALK_KMH * 60);
    }

    static int busMinutes(double roadKm) {
        return (int) Math.ceil(roadKm / BUS_KMH * 60) + BUS_WAIT_MINUTES;
    }

    static Station nearest(List<Station> stations, double lat, double lng) {
        return stations.stream()
                .min(Comparator.comparingDouble(s -> GeoDistance.kilometres(lat, lng, s.lat(), s.lng())))
                .orElseThrow();
    }

    public static Optional<TrainJourney> train(List<Station> stations, double fromLat, double fromLng, double toLat, double toLng) {
        if (stations.isEmpty()) {
            return Optional.empty();
        }
        Station start = nearest(stations, fromLat, fromLng);
        Station end = nearest(stations, toLat, toLng);
        if (start.getName().equals(end.getName())) {
            return Optional.empty(); // the train would not help: you would get on and off at the same station
        }
        int walkTo = walkMinutes(GeoDistance.kilometres(fromLat, fromLng, start.lat(), start.lng()));
        if (walkTo > MAX_WALK_TO_STATION_MINUTES) {
            return Optional.empty();
        }
        int walkFrom = walkMinutes(GeoDistance.kilometres(end.lat(), end.lng(), toLat, toLng));
        double railKm = GeoDistance.kilometres(start.lat(), start.lng(), end.lat(), end.lng()) * RAIL_DETOUR;
        if (railKm < MIN_RAIL_KM) {
            return Optional.empty(); // a one-stop hop: not worth the walk and the wait
        }
        int ride = (int) Math.ceil(railKm / TRAIN_KMH * 60);
        return Optional.of(new TrainJourney(start.getName(), end.getName(), walkTo, TRAIN_WAIT_MINUTES, ride, walkFrom,
                GeoDistance.rounded(railKm)));
    }
}
