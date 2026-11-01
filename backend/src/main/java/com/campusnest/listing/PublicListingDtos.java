package com.campusnest.listing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.campusnest.listing.ListingDtos.AmenityView;
import com.campusnest.listing.ListingDtos.ChargeView;
import com.campusnest.listing.ListingDtos.ImageView;
import com.campusnest.listing.ListingDtos.RoomView;
import com.campusnest.listing.ListingTypes.GenderPolicy;
import com.campusnest.listing.ListingTypes.PropertyType;
import com.campusnest.listing.ListingTypes.RoomType;
import com.campusnest.owner.OwnerType;
import com.campusnest.owner.VerificationStatus;

/**
 * What students and visitors may see about a published listing. Deliberately NO owner email or phone:
 * contact happens through enquiries, where each side chooses whether to share contact details.
 */
public final class PublicListingDtos {

    private PublicListingDtos() {
    }

    /** How recently the owner confirmed availability (thresholds documented in docs/ALGORITHMS.md). */
    public enum Freshness {
        /** confirmed within 14 days */
        FRESH,
        /** 15–30 days */
        AGING,
        /** more than 30 days, or never */
        STALE
    }

    public enum SortOrder {
        RENT_ASC,
        DISTANCE_ASC,
        RECENTLY_CONFIRMED
    }

    /** Who a student is searching for; listings restricted to the other gender are excluded. */
    public enum SuitableFor {
        FEMALE,
        MALE
    }

    public record SearchCriteria(
            String q,
            Integer maxRent,
            RoomType roomType,
            SuitableFor suitableFor,
            List<String> amenities,
            boolean availableOnly,
            Long collegeId,
            Double maxDistanceKm,
            SortOrder sort,
            int page) {
    }

    public record ListingCard(
            Long id,
            String title,
            String locality,
            PropertyType propertyType,
            GenderPolicy genderPolicy,
            /** Lowest rent among the rooms that match the filters. */
            int fromRent,
            List<RoomType> roomTypes,
            int bedsAvailable,
            BigDecimal latitude,
            BigDecimal longitude,
            /** Straight-line distance to the chosen college; null when no college was given. */
            Double distanceKm,
            String coverImageUrl,
            Freshness freshness,
            Instant lastConfirmedAt,
            VerificationStatus ownerVerification,
            boolean sampleData) {
    }

    public record OwnerPublic(String displayName, OwnerType ownerType, String businessName,
            VerificationStatus verificationStatus) {
    }

    public record PublicListingDetail(
            Long id,
            String title,
            String description,
            PropertyType propertyType,
            GenderPolicy genderPolicy,
            String addressLine,
            String locality,
            String city,
            String pincode,
            BigDecimal latitude,
            BigDecimal longitude,
            ListingStatus status,
            Instant publishedAt,
            Instant lastConfirmedAt,
            Freshness freshness,
            boolean sampleData,
            List<RoomView> rooms,
            List<ChargeView> charges,
            List<AmenityView> amenities,
            List<ImageView> images,
            OwnerPublic owner) {
    }
}
