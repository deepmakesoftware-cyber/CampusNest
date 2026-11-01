package com.campusnest.listing;

/**
 * Small fixed lists of values used by listings. Kept together in one file because each is only a few words;
 * the database has a matching CHECK constraint for every one (see V2__listings.sql).
 */
public final class ListingTypes {

    private ListingTypes() {
    }

    public enum PropertyType {
        PG,
        HOSTEL,
        FLAT,
        ROOM
    }

    /** Many Mumbai PGs accept only one gender; students are matched against this. */
    public enum GenderPolicy {
        ANY,
        FEMALE_ONLY,
        MALE_ONLY
    }

    public enum RoomType {
        SINGLE,
        DOUBLE,
        TRIPLE,
        DORMITORY
    }

    public enum ChargeType {
        ELECTRICITY,
        MAINTENANCE,
        FOOD,
        WIFI,
        WATER,
        OTHER
    }

    public enum ChargeFrequency {
        MONTHLY,
        ONE_TIME
    }
}
