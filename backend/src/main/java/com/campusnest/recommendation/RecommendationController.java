package com.campusnest.recommendation;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusnest.auth.AppUserPrincipal;
import com.campusnest.recommendation.RecommendationService.RecommendedListing;
import com.campusnest.recommendation.RecommendationService.Recommendations;

/** Ranked, explained recommendations for the logged-in student (STUDENT area). */
@RestController
@RequestMapping("/api/v1/student/recommendations")
public class RecommendationController {

    private final RecommendationService service;

    public RecommendationController(RecommendationService service) {
        this.service = service;
    }

    /** 409 PREFERENCES_REQUIRED / COLLEGE_REQUIRED if the student has not set them yet. */
    @GetMapping
    public Recommendations recommend(@AuthenticationPrincipal AppUserPrincipal me) {
        return service.recommend(me.getId());
    }

    @GetMapping("/listings/{listingId}")
    public RecommendedListing forListing(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long listingId) {
        return service.forListing(me.getId(), listingId);
    }
}
