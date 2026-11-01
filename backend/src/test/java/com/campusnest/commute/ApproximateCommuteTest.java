package com.campusnest.commute;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApproximateCommuteTest {

    // VJTI
    static final double LAT = 19.022172;
    static final double LNG = 72.856204;

    @Test
    void shortTripsAreWalkedWhenModeIsAny() {
        CommuteEstimate e = ApproximateCommute.estimate(19.0262, 72.8566, LAT, LNG, TravelMode.ANY); // ~0.45 km
        assertThat(e.mode()).isEqualTo(TravelMode.WALK);
        assertThat(e.approximate()).isTrue();
        assertThat(e.minutes()).isBetween(6, 9);
    }

    @Test
    void longerTripsUseAnAutoRickshawWithWaitingTime() {
        CommuteEstimate e = ApproximateCommute.estimate(19.0620, 72.8990, LAT, LNG, TravelMode.ANY); // Chembur ~6.3 km
        assertThat(e.mode()).isEqualTo(TravelMode.AUTO_RICKSHAW);
        // 6.3 × 1.3 = 8.2 km at 18 km/h = 28 min + 5 min waiting
        assertThat(e.minutes()).isBetween(31, 35);
        assertThat(e.method()).contains("straight-line");
    }

    @Test
    void explicitWalkPreferenceIsRespectedEvenForLongTrips() {
        CommuteEstimate e = ApproximateCommute.estimate(19.0620, 72.8990, LAT, LNG, TravelMode.WALK);
        assertThat(e.mode()).isEqualTo(TravelMode.WALK);
        assertThat(e.minutes()).isGreaterThan(100);
    }

    @Test
    void trainPreferenceFallsBackToRoadUntilStationDataExists() {
        CommuteEstimate e = ApproximateCommute.estimate(19.0620, 72.8990, LAT, LNG, TravelMode.LOCAL_TRAIN);
        assertThat(e.mode()).isEqualTo(TravelMode.AUTO_RICKSHAW);
    }
}
