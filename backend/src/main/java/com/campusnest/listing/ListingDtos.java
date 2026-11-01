package com.campusnest.listing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.campusnest.auth.Role;
import com.campusnest.listing.ListingTypes.ChargeFrequency;
import com.campusnest.listing.ListingTypes.ChargeType;
import com.campusnest.listing.ListingTypes.GenderPolicy;
import com.campusnest.listing.ListingTypes.PropertyType;
import com.campusnest.listing.ListingTypes.RoomType;
import com.campusnest.owner.OwnerType;
import com.campusnest.owner.VerificationStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request and response bodies for listing endpoints (owner and admin). */
public final class ListingDtos {

    private ListingDtos() {
    }

    // ---------------- Requests ----------------

    public record CreateListingRequest(@NotBlank @Size(min = 5, max = 120) String title) {
    }

    /**
     * Full listing content, sent by the owner's edit form. Rooms with an id are updated, rooms without one are
     * added, and existing rooms missing from the list are removed. Amenities left out mean "not specified".
     */
    public record ListingRequest(
            @NotBlank @Size(min = 5, max = 120) String title,
            @Size(max = 4000) String description,
            @NotNull PropertyType propertyType,
            @NotNull GenderPolicy genderPolicy,
            @Size(max = 300) String addressLine,
            @Size(max = 100) String locality,
            @Pattern(regexp = "^$|^[1-9][0-9]{5}$", message = "must be a 6-digit PIN code") String pincode,
            @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
            @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
            @NotNull @Size(max = 10) List<@Valid @NotNull RoomRequest> rooms,
            @NotNull @Size(max = 15) List<@Valid @NotNull ChargeRequest> charges,
            @NotNull List<@Valid @NotNull AmenityAnswer> amenities) {
    }

    public record RoomRequest(
            Long id,
            @NotNull RoomType roomType,
            @NotNull @Min(1) @Max(12) Integer sharingCapacity,
            @NotNull @Min(1) @Max(1_000_000) Integer monthlyRent,
            @Min(0) @Max(10_000_000) Integer securityDeposit,
            @NotNull @Min(1) @Max(500) Integer bedsTotal,
            @NotNull @Min(0) @Max(500) Integer bedsAvailable) {

        @AssertTrue(message = "available beds cannot be more than total beds")
        public boolean isBedsConsistent() {
            return bedsTotal == null || bedsAvailable == null || bedsAvailable <= bedsTotal;
        }
    }

    public record ChargeRequest(
            @NotNull ChargeType chargeType,
            @Size(max = 100) String description,
            @NotNull @Min(0) @Max(1_000_000) Integer amount,
            @NotNull ChargeFrequency frequency,
            boolean mandatory) {
    }

    public record AmenityAnswer(@NotNull Long amenityId, @NotNull Boolean available) {
    }

    /** Owner confirms or updates free beds for each room type. */
    public record AvailabilityRequest(@NotNull @Size(min = 1, max = 10) List<@Valid @NotNull RoomAvailability> rooms) {
    }

    public record RoomAvailability(@NotNull Long roomId, @NotNull @Min(0) @Max(500) Integer bedsAvailable) {
    }

    /** Admin decisions that must be explained to the owner. */
    public record ReasonRequest(@NotBlank @Size(max = 1000) String reason) {
    }

    // ---------------- Responses ----------------

    public record ListingDetail(
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
            String statusReason,
            Instant submittedAt,
            Instant publishedAt,
            Instant lastConfirmedAt,
            boolean sampleData,
            List<RoomView> rooms,
            List<ChargeView> charges,
            List<AmenityView> amenities,
            List<ImageView> images,
            /** Empty when the listing is complete enough to submit. */
            List<String> missingForSubmission,
            boolean ownerCanEdit,
            List<StatusChangeView> history) {
    }

    public record RoomView(Long id, RoomType roomType, int sharingCapacity, int monthlyRent, Integer securityDeposit,
            int bedsTotal, int bedsAvailable, Instant availabilityUpdatedAt) {
    }

    public record ChargeView(ChargeType chargeType, String description, int amount, ChargeFrequency frequency,
            boolean mandatory) {
    }

    /** One entry per amenity in the catalogue; available is null when the owner has not answered. */
    public record AmenityView(Long amenityId, String code, String name, boolean core, Boolean available) {
    }

    public record ImageView(Long id, String url, int width, int height) {
    }

    /** History as shown to owners and admins: the actor's role, not their identity. */
    public record StatusChangeView(ListingStatus fromStatus, ListingStatus toStatus, String reason, Instant at,
            Role actorRole) {
    }

    public record ListingSummary(Long id, String title, String locality, ListingStatus status, String statusReason,
            Integer minimumRent, int roomTypes, int images, Instant updatedAt) {
    }

    public record AdminListingSummary(Long id, String title, String locality, ListingStatus status,
            String ownerName, VerificationStatus ownerVerification, Integer minimumRent, Instant submittedAt,
            Instant updatedAt) {
    }

    /** Owner details an admin needs to review a listing (phone included so the admin can contact them). */
    public record OwnerInfo(Long id, String displayName, String email, String phone, OwnerType ownerType,
            String businessName, VerificationStatus verificationStatus) {
    }

    public record AdminListingDetail(ListingDetail listing, OwnerInfo owner) {
    }
}
