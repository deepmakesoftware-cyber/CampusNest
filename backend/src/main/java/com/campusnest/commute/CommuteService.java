package com.campusnest.commute;

import org.springframework.stereotype.Service;

import com.campusnest.college.College;
import com.campusnest.listing.Listing;

/**
 * The single entry point other modules use for commute estimates (recommendations, comparison).
 * Phase 5: approximate estimate from straight-line distance (ApproximateCommute).
 * Phase 6: adds better sources and keeps ApproximateCommute as the always-available fallback.
 */
@Service
public class CommuteService {

    /** Returns null only if the listing has no location (never the case for published listings). */
    public CommuteEstimate estimate(Listing listing, College college, TravelMode preferred) {
        if (listing.getLatitude() == null || listing.getLongitude() == null) {
            return null;
        }
        return ApproximateCommute.estimate(listing.getLatitude().doubleValue(), listing.getLongitude().doubleValue(),
                college.getLatitude().doubleValue(), college.getLongitude().doubleValue(),
                preferred == null ? TravelMode.ANY : preferred);
    }
}
