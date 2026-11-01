package com.campusnest.listing;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public list of amenities (used by the listing form and, later, by search filters). */
@RestController
public class AmenityController {

    private final AmenityRepository amenities;

    public AmenityController(AmenityRepository amenities) {
        this.amenities = amenities;
    }

    @GetMapping("/api/v1/amenities")
    public List<AmenityResponse> list() {
        return amenities.findAllByOrderBySortOrderAsc().stream()
                .map(a -> new AmenityResponse(a.getId(), a.getCode(), a.getName(), a.isCore()))
                .toList();
    }

    public record AmenityResponse(Long id, String code, String name, boolean core) {
    }
}
