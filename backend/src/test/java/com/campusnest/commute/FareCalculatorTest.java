package com.campusnest.commute;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Fare maths, using the same rules as the V5 migration (auto: ₹27 first 1.5 km + ₹18.22/km; BEST non-AC slabs). */
class FareCalculatorTest {

    static List<FareRule> seededRules() {
        List<FareRule> rules = new ArrayList<>();
        rules.add(new FareRule(TravelMode.AUTO_RICKSHAW, FareRule.Kind.PER_TRIP_METERED, null, new BigDecimal("27.00"),
                new BigDecimal("1.50"), new BigDecimal("18.22"), "auto", "MMRTA", null, LocalDate.of(2026, 9, 1)));
        int[][] bus = {{5, 10}, {10, 15}, {15, 20}, {20, 30}, {25, 35}};
        for (int[] b : bus) {
            rules.add(new FareRule(TravelMode.BUS, FareRule.Kind.PER_TRIP_SLAB, BigDecimal.valueOf(b[0]), BigDecimal.valueOf(b[1]),
                    null, null, "bus", "BEST", null, LocalDate.of(2025, 5, 9)));
        }
        return rules;
    }

    private final FareCalculator calc = new FareCalculator(seededRules());

    @Test
    void autoMinimumFareCoversFirstOneAndAHalfKilometres() {
        assertThat(calc.autoRickshawTrip(0.5).orElseThrow().rupees()).isEqualTo(27);
        assertThat(calc.autoRickshawTrip(1.5).orElseThrow().rupees()).isEqualTo(27);
    }

    @Test
    void autoChargesPerKilometreAfterTheMinimum() {
        // 27 + (3.0 − 1.5) × 18.22 = 54.33 -> 54
        assertThat(calc.autoRickshawTrip(3.0).orElseThrow().rupees()).isEqualTo(54);
        // 27 + (8.2 − 1.5) × 18.22 = 149.07 -> 149
        assertThat(calc.autoRickshawTrip(8.2).orElseThrow().rupees()).isEqualTo(149);
        assertThat(calc.autoRickshawTrip(3.0).orElseThrow().source()).isEqualTo("MMRTA");
    }

    @Test
    void busUsesTheSmallestSlabThatCoversTheDistance() {
        assertThat(calc.busTrip(4.0).orElseThrow().rupees()).isEqualTo(10);
        assertThat(calc.busTrip(5.0).orElseThrow().rupees()).isEqualTo(10);
        assertThat(calc.busTrip(5.1).orElseThrow().rupees()).isEqualTo(15);
        assertThat(calc.busTrip(25.0).orElseThrow().rupees()).isEqualTo(35);
        assertThat(calc.busTrip(25.1)).isEmpty(); // no published slab -> no guess
    }

    @Test
    void trainPassIsNotAvailableUntilAFareIsEntered() {
        assertThat(calc.trainMonthlyPass(10)).isEmpty();

        List<FareRule> withPass = new ArrayList<>(seededRules());
        withPass.add(new FareRule(TravelMode.LOCAL_TRAIN, FareRule.Kind.MONTHLY_PASS_SLAB, BigDecimal.valueOf(10),
                BigDecimal.valueOf(150), null, null, "test pass", "test source", null, LocalDate.of(2026, 1, 1)));
        assertThat(new FareCalculator(withPass).trainMonthlyPass(9.5).orElseThrow().rupees()).isEqualTo(150);
        assertThat(new FareCalculator(withPass).trainMonthlyPass(12)).isEmpty();
    }

    @Test
    void noRulesMeansNoFares() {
        FareCalculator empty = new FareCalculator(List.of());
        assertThat(empty.autoRickshawTrip(3)).isEmpty();
        assertThat(empty.busTrip(3)).isEmpty();
    }
}
