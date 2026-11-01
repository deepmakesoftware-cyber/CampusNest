package com.campusnest.college;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public list of supported colleges (used by registration and profile forms). */
@RestController
@RequestMapping("/api/v1/colleges")
public class CollegeController {

    private final CollegeRepository colleges;

    public CollegeController(CollegeRepository colleges) {
        this.colleges = colleges;
    }

    @GetMapping
    public List<CollegeResponse> list() {
        return colleges.findAllByOrderByNameAsc().stream().map(CollegeResponse::from).toList();
    }

    public record CollegeResponse(Long id, String name, String shortName, String address, String city,
            BigDecimal latitude, BigDecimal longitude) {

        static CollegeResponse from(College c) {
            return new CollegeResponse(c.getId(), c.getName(), c.getShortName(), c.getAddress(), c.getCity(),
                    c.getLatitude(), c.getLongitude());
        }
    }
}
