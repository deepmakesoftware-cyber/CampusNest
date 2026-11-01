package com.campusnest.commute;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.campusnest.commute.CommuteModel.TrainJourney;

/** Train journey rules, with a few real station coordinates (OpenStreetMap). */
class CommuteModelTest {

    static final double VJTI_LAT = 19.022172;
    static final double VJTI_LNG = 72.856204;

    static final List<Station> STATIONS = List.of(
            new Station("Matunga", 19.02744, 72.85015),
            new Station("King's Circle", 19.03168, 72.85774),
            new Station("Chembur", 19.062632, 72.90114),
            new Station("Thane", 19.18648, 72.97577));

    @Test
    void trainFromChemburIsBrokenIntoExplainableLegs() {
        TrainJourney j = CommuteModel.train(STATIONS, 19.0620, 72.8990, VJTI_LAT, VJTI_LNG).orElseThrow();
        assertThat(j.fromStation()).isEqualTo("Chembur");
        assertThat(j.toStation()).isEqualTo("Matunga"); // nearest station to VJTI in this list
        assertThat(j.waitMinutes()).isEqualTo(CommuteModel.TRAIN_WAIT_MINUTES);
        assertThat(j.totalMinutes()).isEqualTo(j.walkToStationMinutes() + j.waitMinutes() + j.rideMinutes() + j.walkFromStationMinutes());
        assertThat(j.railKm()).isGreaterThan(6).isLessThan(9);
    }

    @Test
    void noTrainWhenBothEndsUseTheSameStation() {
        // Next to Matunga station: the nearest station is Matunga at both ends.
        assertThat(CommuteModel.train(STATIONS, 19.0270, 72.8505, VJTI_LAT, VJTI_LNG)).isEmpty();
    }

    @Test
    void noTrainForAOneStopHop() {
        // Near King's Circle: King's Circle -> Matunga is about 1 km of rail, so walking or an auto is better.
        assertThat(CommuteModel.train(STATIONS, 19.0262, 72.8566, VJTI_LAT, VJTI_LNG)).isEmpty();
    }

    @Test
    void noTrainWhenTheStationIsTooFarToWalk() {
        // A point several km from every station in the list.
        assertThat(CommuteModel.train(STATIONS, 19.12, 72.83, VJTI_LAT, VJTI_LNG)).isEmpty();
    }

    @Test
    void noStationsMeansNoTrainEstimate() {
        assertThat(CommuteModel.train(List.of(), 19.0620, 72.8990, VJTI_LAT, VJTI_LNG)).isEmpty();
    }

    @Test
    void busTimeIncludesWaiting() {
        assertThat(CommuteModel.busMinutes(13.0)).isEqualTo(60 + CommuteModel.BUS_WAIT_MINUTES);
    }
}
