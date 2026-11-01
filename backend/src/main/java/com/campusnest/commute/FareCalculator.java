package com.campusnest.commute;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Turns distances into ESTIMATED fares using only the stored fare rules (with their sources). If no rule covers a
 * case, the result is empty and the UI says "not available": no fare is ever guessed.
 * Plain Java (no Spring), so it is easy to unit-test.
 */
public final class FareCalculator {

    /** A fare together with where its rule came from. */
    public record Fare(int rupees, String rule, String source) {
    }

    private final List<FareRule> rules;

    public FareCalculator(List<FareRule> rules) {
        this.rules = List.copyOf(rules);
    }

    /** One auto-rickshaw trip: minimum fare for the first base_km, then per_km, rounded to the nearest rupee. */
    public Optional<Fare> autoRickshawTrip(double roadKm) {
        return rules.stream()
                .filter(r -> r.getMode() == TravelMode.AUTO_RICKSHAW && r.getKind() == FareRule.Kind.PER_TRIP_METERED)
                .max(Comparator.comparing(FareRule::getEffectiveFrom))
                .map(r -> {
                    double extraKm = Math.max(0, roadKm - r.getBaseKm().doubleValue());
                    double fare = r.getBaseFare().doubleValue() + extraKm * r.getPerKm().doubleValue();
                    return new Fare((int) Math.round(fare), r.getDescription(), r.getSourceName());
                });
    }

    /** One bus trip: the fare of the smallest slab that covers the distance. */
    public Optional<Fare> busTrip(double roadKm) {
        return slab(TravelMode.BUS, FareRule.Kind.PER_TRIP_SLAB, roadKm);
    }

    /** A monthly local-train pass for this rail distance, if an administrator has entered pass fares. */
    public Optional<Fare> trainMonthlyPass(double railKm) {
        return slab(TravelMode.LOCAL_TRAIN, FareRule.Kind.MONTHLY_PASS_SLAB, railKm);
    }

    private Optional<Fare> slab(TravelMode mode, FareRule.Kind kind, double km) {
        BigDecimal distance = BigDecimal.valueOf(km);
        return rules.stream()
                .filter(r -> r.getMode() == mode && r.getKind() == kind)
                .filter(r -> r.getUpToKm().compareTo(distance) >= 0)
                .min(Comparator.comparing(FareRule::getUpToKm))
                .map(r -> new Fare(r.getBaseFare().setScale(0, java.math.RoundingMode.HALF_UP).intValue(),
                        r.getDescription(), r.getSourceName()));
    }
}
