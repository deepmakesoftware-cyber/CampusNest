package com.campusnest.commute;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusnest.college.College;
import com.campusnest.common.geo.GeoDistance;
import com.campusnest.commute.CommuteModel.TrainJourney;
import com.campusnest.commute.FareCalculator.Fare;
import com.campusnest.listing.Listing;

/**
 * The single entry point for commute estimates. For each listing it produces one option per travel mode
 * (walk, auto-rickshaw, bus, local train) with an estimated time and, where a sourced fare rule exists,
 * an estimated monthly cost. All results are approximate and say how they were made.
 */
@Service
public class CommuteService {

    public static final int DEFAULT_DAYS_PER_MONTH = 22;

    private final StationRepository stations;
    private final FareRuleRepository fareRules;

    public CommuteService(StationRepository stations, FareRuleRepository fareRules) {
        this.stations = stations;
        this.fareRules = fareRules;
    }

    /**
     * One travel option.
     *
     * @param monthlyCost estimated cost for 2 trips per college day, or null when no sourced fare is available
     * @param costNote    how the cost was calculated (or why it is not available)
     */
    public record CommuteOption(TravelMode mode, boolean available, int minutes, double distanceKm, Integer monthlyCost,
            String costNote, String method, String unavailableReason) {
    }

    /** All four modes for one listing. Loads stations and fares itself; for many listings use {@link #context()}. */
    @Transactional(readOnly = true)
    public List<CommuteOption> options(Listing listing, College college, int daysPerMonth) {
        return options(context(), listing, college, daysPerMonth);
    }

    /** Reference data loaded once and reused for a whole batch of listings. */
    public record Context(List<Station> stations, FareCalculator fares) {
    }

    @Transactional(readOnly = true)
    public Context context() {
        return new Context(stations.findAll(), new FareCalculator(fareRules.findAll()));
    }

    public List<CommuteOption> options(Context ctx, Listing listing, College college, int daysPerMonth) {
        if (listing.getLatitude() == null || listing.getLongitude() == null) {
            return List.of();
        }
        double fromLat = listing.getLatitude().doubleValue();
        double fromLng = listing.getLongitude().doubleValue();
        double toLat = college.getLatitude().doubleValue();
        double toLng = college.getLongitude().doubleValue();
        double straight = GeoDistance.kilometres(fromLat, fromLng, toLat, toLng);
        double road = GeoDistance.rounded(straight * ApproximateCommute.DETOUR_FACTOR);
        int trips = 2 * daysPerMonth;
        List<CommuteOption> result = new ArrayList<>();

        // Walk
        int walk = CommuteModel.walkMinutes(straight);
        result.add(new CommuteOption(TravelMode.WALK, true, walk, road, 0, "Free",
                "Walking " + road + " km at an assumed 4.5 km/h.", null));

        // Auto-rickshaw
        CommuteEstimate auto = ApproximateCommute.estimate(fromLat, fromLng, toLat, toLng, TravelMode.AUTO_RICKSHAW);
        Optional<Fare> autoFare = ctx.fares().autoRickshawTrip(road);
        result.add(new CommuteOption(TravelMode.AUTO_RICKSHAW, true, auto.minutes(), road,
                autoFare.map(f -> f.rupees() * trips).orElse(null),
                autoFare.map(f -> "₹" + f.rupees() + " per trip × " + trips + " trips (" + f.rule() + "; " + f.source() + ")")
                        .orElse("Fare not available: no auto-rickshaw fare rule entered."),
                auto.method() + ".", null));

        // Bus
        Optional<Fare> busFare = ctx.fares().busTrip(road);
        result.add(new CommuteOption(TravelMode.BUS, busFare.isPresent(), CommuteModel.busMinutes(road), road,
                busFare.map(f -> f.rupees() * trips).orElse(null),
                busFare.map(f -> "₹" + f.rupees() + " per trip × " + trips + " trips (" + f.rule() + "; " + f.source() + ")")
                        .orElse("Fare not available for this distance."),
                "Road distance at an assumed 13 km/h plus 10 min waiting. Assumes a direct BEST route; check the route before relying on it.",
                busFare.isPresent() ? null : "No bus fare rule covers " + road + " km."));

        // Local train
        Optional<TrainJourney> journey = CommuteModel.train(ctx.stations(), fromLat, fromLng, toLat, toLng);
        if (journey.isEmpty()) {
            result.add(new CommuteOption(TravelMode.LOCAL_TRAIN, false, 0, 0, null, "—", null,
                    "No useful train journey: the nearest stations are the same, or the station is too far to walk."));
        } else {
            TrainJourney j = journey.get();
            Optional<Fare> pass = ctx.fares().trainMonthlyPass(j.railKm());
            result.add(new CommuteOption(TravelMode.LOCAL_TRAIN, true, j.totalMinutes(), j.railKm(),
                    pass.map(Fare::rupees).orElse(null),
                    pass.map(f -> "Monthly pass (" + f.rule() + "; " + f.source() + ")")
                            .orElse("Pass cost not available: no verified local-train fare has been entered yet."),
                    "Walk " + j.walkToStationMinutes() + " min to " + j.fromStation() + ", about " + j.waitMinutes()
                            + " min wait, " + j.rideMinutes() + " min to " + j.toStation() + " (" + j.railKm()
                            + " km by rail, assumed 30 km/h), walk " + j.walkFromStationMinutes() + " min.",
                    null));
        }
        return result;
    }

    /**
     * The commute used by the suitability score: the student's chosen mode if possible; for "any", the fastest
     * practical option (walking only if under 30 minutes; bus is not auto-picked because routes are unknown).
     */
    @Transactional(readOnly = true)
    public CommuteEstimate estimate(Listing listing, College college, TravelMode preferred) {
        return estimate(context(), listing, college, preferred);
    }

    public CommuteEstimate estimate(Context ctx, Listing listing, College college, TravelMode preferred) {
        List<CommuteOption> all = options(ctx, listing, college, DEFAULT_DAYS_PER_MONTH);
        if (all.isEmpty()) {
            return null;
        }
        TravelMode wanted = preferred == null ? TravelMode.ANY : preferred;
        Optional<CommuteOption> chosen = all.stream().filter(o -> o.available() && o.mode() == wanted).findFirst();
        String note = "";
        if (chosen.isEmpty()) {
            if (wanted != TravelMode.ANY) {
                note = " (" + wanted.name().toLowerCase().replace('_', ' ') + " not available here)";
            }
            chosen = all.stream()
                    .filter(CommuteOption::available)
                    .filter(o -> o.mode() != TravelMode.BUS)
                    .filter(o -> o.mode() != TravelMode.WALK || o.minutes() <= CommuteModel.MAX_PRACTICAL_WALK_MINUTES)
                    .min(Comparator.comparingInt(CommuteOption::minutes));
        }
        CommuteOption o = chosen.orElseThrow();
        return new CommuteEstimate(o.mode(), o.distanceKm(), Math.max(o.minutes(), 1), true, o.method() + note);
    }
}
