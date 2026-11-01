package com.campusnest.listing;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campusnest.common.web.PageResponse;
import com.campusnest.listing.ListingTypes.RoomType;
import com.campusnest.listing.PublicListingDtos.ListingCard;
import com.campusnest.listing.PublicListingDtos.PublicListingDetail;
import com.campusnest.listing.PublicListingDtos.SearchCriteria;
import com.campusnest.listing.PublicListingDtos.SortOrder;
import com.campusnest.listing.PublicListingDtos.SuitableFor;

/** Public browsing: anyone can search and view published listings (no login needed). */
@RestController
@RequestMapping("/api/v1/listings")
public class PublicListingController {

    private final PublicListingService service;

    public PublicListingController(PublicListingService service) {
        this.service = service;
    }

    /**
     * Example: GET /api/v1/listings?collegeId=1&maxRent=10000&roomType=DOUBLE&suitableFor=FEMALE
     *          &amenities=WIFI&amenities=MEALS&availableOnly=true&maxDistanceKm=5&sort=DISTANCE_ASC&page=0
     * Every parameter is optional.
     */
    @GetMapping
    public PageResponse<ListingCard> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer maxRent,
            @RequestParam(required = false) RoomType roomType,
            @RequestParam(required = false) SuitableFor suitableFor,
            @RequestParam(required = false) List<String> amenities,
            @RequestParam(defaultValue = "false") boolean availableOnly,
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) Double maxDistanceKm,
            @RequestParam(required = false) SortOrder sort,
            @RequestParam(defaultValue = "0") int page) {
        return service.search(new SearchCriteria(q, maxRent, roomType, suitableFor, amenities, availableOnly,
                collegeId, maxDistanceKm, sort, page));
    }

    @GetMapping("/{id}")
    public PublicListingDetail get(@PathVariable Long id) {
        return service.get(id);
    }
}
