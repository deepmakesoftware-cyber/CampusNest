package com.campusnest.commute;

/**
 * An ESTIMATED one-way trip from a listing to a college.
 *
 * @param approximate true when based on straight-line distance and assumed speeds (always labelled in the UI)
 * @param method      a short human-readable description of how the estimate was made
 */
public record CommuteEstimate(TravelMode mode, double distanceKm, int minutes, boolean approximate, String method) {
}
