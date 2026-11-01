package com.campusnest.recommendation;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campusnest.auth.AppUserPrincipal;
import com.campusnest.commute.CommuteService;
import com.campusnest.recommendation.ComparisonService.Comparison;

@RestController
public class ComparisonController {

    private final ComparisonService service;

    public ComparisonController(ComparisonService service) {
        this.service = service;
    }

    /** GET /api/v1/student/compare?ids=3,7,9&daysPerMonth=22 */
    @GetMapping("/api/v1/student/compare")
    public Comparison compare(@AuthenticationPrincipal AppUserPrincipal me, @RequestParam List<Long> ids,
            @RequestParam(defaultValue = "" + CommuteService.DEFAULT_DAYS_PER_MONTH) int daysPerMonth) {
        return service.compare(me.getId(), ids, daysPerMonth);
    }
}
