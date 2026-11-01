package com.campusnest.common.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class GeoDistanceTest {

    @Test
    void samePointIsZero() {
        assertThat(GeoDistance.kilometres(19.0222, 72.8562, 19.0222, 72.8562)).isZero();
    }

    @Test
    void oneDegreeOfLatitudeIsAbout111Km() {
        assertThat(GeoDistance.kilometres(19.0, 72.85, 20.0, 72.85)).isCloseTo(111.2, within(0.2));
    }

    @Test
    void vjtiToNearDadarIsAboutOneKilometreStraightLine() {
        // VJTI (19.0222, 72.8562) to a point near Dadar station (19.0178, 72.8478)
        double km = GeoDistance.kilometres(19.0222, 72.8562, 19.0178, 72.8478);
        assertThat(km).isBetween(0.9, 1.1);
    }

    @Test
    void distanceIsSymmetric() {
        double ab = GeoDistance.kilometres(19.0222, 72.8562, 19.1860, 72.9750);
        double ba = GeoDistance.kilometres(19.1860, 72.9750, 19.0222, 72.8562);
        assertThat(ab).isCloseTo(ba, within(1e-9));
    }

    @Test
    void roundsToOneDecimal() {
        assertThat(GeoDistance.rounded(2.4567)).isEqualTo(2.5);
    }
}
