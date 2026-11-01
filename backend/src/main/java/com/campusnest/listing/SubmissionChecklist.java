package com.campusnest.listing;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * What a listing must contain before it can be submitted for review. Returns the missing items
 * (field -> message); an empty result means "ready to submit".
 *
 * Rules are strict on purpose: the suitability score (Phase 5) relies on complete core data,
 * so we refuse incomplete listings instead of guessing later.
 */
public final class SubmissionChecklist {

    static final int MIN_DESCRIPTION_LENGTH = 30;

    // Rough bounding box of the Mumbai Metropolitan Region. v1 supports Mumbai only.
    static final BigDecimal MIN_LAT = new BigDecimal("18.85");
    static final BigDecimal MAX_LAT = new BigDecimal("19.55");
    static final BigDecimal MIN_LNG = new BigDecimal("72.70");
    static final BigDecimal MAX_LNG = new BigDecimal("73.30");

    private SubmissionChecklist() {
    }

    public static Map<String, String> missingItems(Listing listing, List<Amenity> allAmenities) {
        Map<String, String> missing = new LinkedHashMap<>();

        if (listing.getDescription() == null || listing.getDescription().length() < MIN_DESCRIPTION_LENGTH) {
            missing.put("description", "Add a description of at least " + MIN_DESCRIPTION_LENGTH + " characters.");
        }
        if (listing.getAddressLine() == null || listing.getLocality() == null) {
            missing.put("address", "Add the street address and locality.");
        }
        if (listing.getPincode() == null) {
            missing.put("pincode", "Add the 6-digit PIN code.");
        }
        if (listing.getLatitude() == null || listing.getLongitude() == null) {
            missing.put("location", "Mark the property's location on the map.");
        } else if (!insideMumbaiRegion(listing.getLatitude(), listing.getLongitude())) {
            missing.put("location", "The location must be in the Mumbai region (CampusNest currently covers Mumbai only).");
        }
        if (listing.rooms().isEmpty()) {
            missing.put("rooms", "Add at least one room type with its rent.");
        }
        if (listing.imageList().isEmpty()) {
            missing.put("images", "Add at least one photo.");
        }

        Set<Long> answered = listing.amenityAnswers().stream()
                .map(a -> a.getAmenity().getId())
                .collect(Collectors.toSet());
        String unanswered = allAmenities.stream()
                .filter(a -> a.isCore() && !answered.contains(a.getId()))
                .map(Amenity::getName)
                .collect(Collectors.joining(", "));
        if (!unanswered.isEmpty()) {
            missing.put("amenities", "Answer yes or no for: " + unanswered + ".");
        }
        return missing;
    }

    static boolean insideMumbaiRegion(BigDecimal lat, BigDecimal lng) {
        return lat.compareTo(MIN_LAT) >= 0 && lat.compareTo(MAX_LAT) <= 0
                && lng.compareTo(MIN_LNG) >= 0 && lng.compareTo(MAX_LNG) <= 0;
    }
}
