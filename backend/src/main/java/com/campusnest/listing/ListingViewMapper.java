package com.campusnest.listing;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.campusnest.listing.ListingDtos.AmenityView;
import com.campusnest.listing.ListingDtos.ChargeView;
import com.campusnest.listing.ListingDtos.ImageView;
import com.campusnest.listing.ListingDtos.ListingDetail;
import com.campusnest.listing.ListingDtos.ListingSummary;
import com.campusnest.listing.ListingDtos.RoomView;
import com.campusnest.listing.ListingDtos.StatusChangeView;

/** Turns Listing entities into response records. Must be called inside a transaction (lazy collections). */
@Component
public class ListingViewMapper {

    public static final String IMAGE_URL_PREFIX = "/api/v1/listing-images/";

    private final AmenityRepository amenities;
    private final ListingStatusChangeRepository history;

    public ListingViewMapper(AmenityRepository amenities, ListingStatusChangeRepository history) {
        this.amenities = amenities;
        this.history = history;
    }

    public ListingDetail toDetail(Listing l) {
        List<Amenity> catalogue = amenities.findAllByOrderBySortOrderAsc();
        Map<Long, Boolean> answers = l.amenityAnswers().stream()
                .collect(Collectors.toMap(a -> a.getAmenity().getId(), ListingAmenity::isAvailable));

        return new ListingDetail(
                l.getId(), l.getTitle(), l.getDescription(), l.getPropertyType(), l.getGenderPolicy(),
                l.getAddressLine(), l.getLocality(), l.getCity(), l.getPincode(), l.getLatitude(), l.getLongitude(),
                l.getStatus(), l.getStatusReason(), l.getSubmittedAt(), l.getPublishedAt(), l.getLastConfirmedAt(),
                l.isSampleData(),
                l.rooms().stream().map(ListingViewMapper::toRoomView).toList(),
                l.charges().stream().map(c -> new ChargeView(c.getChargeType(), c.getDescription(), c.getAmount(),
                        c.getFrequency(), c.isMandatory())).toList(),
                catalogue.stream().map(a -> new AmenityView(a.getId(), a.getCode(), a.getName(), a.isCore(),
                        answers.get(a.getId()))).toList(),
                l.imageList().stream().map(ListingViewMapper::toImageView).toList(),
                List.copyOf(SubmissionChecklist.missingItems(l, catalogue).values()),
                ListingLifecycle.isOwnerEditable(l.getStatus()),
                history.findByListingIdOrderByIdAsc(l.getId()).stream()
                        .map(h -> new StatusChangeView(h.getFromStatus(), h.getToStatus(), h.getReason(),
                                h.getCreatedAt(), h.getActor() == null ? null : h.getActor().getRole()))
                        .toList());
    }

    public ListingSummary toSummary(Listing l) {
        return new ListingSummary(l.getId(), l.getTitle(), l.getLocality(), l.getStatus(), l.getStatusReason(),
                minRent(l), l.rooms().size(), l.imageList().size(), l.getUpdatedAt());
    }

    static Integer minRent(Listing l) {
        var min = l.minimumRent();
        return min.isPresent() ? min.getAsInt() : null;
    }

    static RoomView toRoomView(RoomOption r) {
        return new RoomView(r.getId(), r.getRoomType(), r.getSharingCapacity(), r.getMonthlyRent(),
                r.getSecurityDeposit(), r.getBedsTotal(), r.getBedsAvailable(), r.getAvailabilityUpdatedAt());
    }

    static ImageView toImageView(ListingImage i) {
        return new ImageView(i.getId(), IMAGE_URL_PREFIX + i.getId(), i.getWidth(), i.getHeight());
    }

    /** Amenity lookup by id, used when applying an owner's answers. */
    Map<Long, Amenity> amenityCatalogue() {
        return amenities.findAll().stream().collect(Collectors.toMap(Amenity::getId, Function.identity()));
    }
}
