package com.campusnest.engagement;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.campusnest.auth.AppUserPrincipal;
import com.campusnest.engagement.ReviewService.AdminReviewView;
import com.campusnest.engagement.ReviewService.HideRequest;
import com.campusnest.engagement.ReviewService.ListingReviews;
import com.campusnest.engagement.ReviewService.MyReview;
import com.campusnest.engagement.ReviewService.OwnReview;
import com.campusnest.engagement.ReviewService.ReviewRequest;

import jakarta.validation.Valid;

@RestController
public class ReviewController {

    private final ReviewService service;

    public ReviewController(ReviewService service) {
        this.service = service;
    }

    /** Public: visible reviews of a published listing. */
    @GetMapping("/api/v1/listings/{listingId}/reviews")
    public ListingReviews forListing(@PathVariable Long listingId) {
        return service.forListing(listingId);
    }

    @GetMapping("/api/v1/student/listings/{listingId}/review")
    public MyReview mine(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long listingId) {
        return service.mine(me.getId(), listingId);
    }

    @PutMapping("/api/v1/student/listings/{listingId}/review")
    public OwnReview save(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long listingId,
            @Valid @RequestBody ReviewRequest request) {
        return service.save(me.getId(), listingId, request);
    }

    @GetMapping("/api/v1/admin/reviews/{id}")
    public AdminReviewView adminGet(@PathVariable Long id) {
        return service.adminGet(id);
    }

    @PostMapping("/api/v1/admin/reviews/{id}/hide")
    public AdminReviewView hide(@PathVariable Long id, @Valid @RequestBody HideRequest request) {
        return service.hide(id, request.reason());
    }

    @PostMapping("/api/v1/admin/reviews/{id}/unhide")
    public AdminReviewView unhide(@PathVariable Long id) {
        return service.unhide(id);
    }
}
