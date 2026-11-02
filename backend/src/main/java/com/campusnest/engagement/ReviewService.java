package com.campusnest.engagement;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusnest.auth.UserAccountRepository;
import com.campusnest.common.Names;
import com.campusnest.common.error.ApiException;
import com.campusnest.listing.Listing;
import com.campusnest.listing.ListingRepository;
import com.campusnest.listing.PublicListingService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Listing reviews (FR-3). Rule (decision D9): a student may review a listing only after sending an enquiry about it.
 * We cannot prove the student actually stayed there, so every review is labelled "stay not verified" and ratings are
 * never presented as proof that a listing is genuine. One review per student per listing (editable).
 */
@Service
public class ReviewService {

    private final ReviewRepository reviews;
    private final EnquiryRepository enquiries;
    private final ListingRepository listings;
    private final UserAccountRepository users;

    public ReviewService(ReviewRepository reviews, EnquiryRepository enquiries, ListingRepository listings,
            UserAccountRepository users) {
        this.reviews = reviews;
        this.enquiries = enquiries;
        this.listings = listings;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public ListingReviews forListing(Long listingId) {
        Listing l = visible(listingId);
        List<ReviewView> visible = reviews.findByListingIdAndStatusOrderByCreatedAtDesc(l.getId(), Review.VISIBLE).stream()
                .map(ReviewService::toView).toList();
        Double average = visible.isEmpty() ? null
                : Math.round(visible.stream().mapToInt(ReviewView::rating).average().orElse(0) * 10) / 10.0;
        return new ListingReviews(visible.size(), average, visible);
    }

    /** Can this student review this listing, and what did they write before? */
    @Transactional(readOnly = true)
    public MyReview mine(Long studentId, Long listingId) {
        Listing l = visible(listingId);
        boolean eligible = enquiries.findByListingIdAndStudentId(l.getId(), studentId).isPresent();
        return new MyReview(eligible, eligible ? null : "You can review a place after contacting its owner through CampusNest.",
                reviews.findByListingIdAndStudentId(l.getId(), studentId).map(ReviewService::toOwnView).orElse(null));
    }

    @Transactional
    public OwnReview save(Long studentId, Long listingId, ReviewRequest request) {
        Listing l = visible(listingId);
        if (enquiries.findByListingIdAndStudentId(l.getId(), studentId).isEmpty()) {
            throw ApiException.conflict("REVIEW_NOT_ALLOWED", "You can review a place after contacting its owner through CampusNest.");
        }
        Review review = reviews.findByListingIdAndStudentId(l.getId(), studentId)
                .orElseGet(() -> new Review(l, users.getReferenceById(studentId)));
        // Editing does not un-hide a review an administrator hid.
        review.edit(request.rating(), request.comment());
        return toOwnView(reviews.save(review));
    }

    @Transactional
    public AdminReviewView hide(Long reviewId, String reason) {
        Review r = load(reviewId);
        r.hide(reason);
        return toAdminView(r);
    }

    @Transactional
    public AdminReviewView unhide(Long reviewId) {
        Review r = load(reviewId);
        r.unhide();
        return toAdminView(r);
    }

    @Transactional(readOnly = true)
    public AdminReviewView adminGet(Long reviewId) {
        return toAdminView(load(reviewId));
    }

    private Review load(Long id) {
        return reviews.findById(id).orElseThrow(() -> ApiException.notFound("REVIEW_NOT_FOUND", "Review not found."));
    }

    private Listing visible(Long listingId) {
        return listings.findById(listingId).filter(PublicListingService::isVisible)
                .orElseThrow(() -> ApiException.notFound("LISTING_NOT_FOUND", "Listing not found."));
    }

    static ReviewView toView(Review r) {
        return new ReviewView(r.getId(), Names.shortName(r.getStudent().getDisplayName()),
                r.getRating(), r.getComment(), r.getCreatedAt(), false);
    }

    private static OwnReview toOwnView(Review r) {
        return new OwnReview(r.getId(), r.getRating(), r.getComment(), r.isVisible(), r.getUpdatedAt());
    }

    private static AdminReviewView toAdminView(Review r) {
        return new AdminReviewView(r.getId(), r.getListing().getId(), r.getListing().getTitle(),
                r.getStudent().getDisplayName(), r.getRating(), r.getComment(), r.getStatus(), r.getHiddenReason(), r.getCreatedAt());
    }

    // ---------------- DTOs ----------------

    public record ReviewRequest(@NotNull @Min(1) @Max(5) Integer rating, @Size(max = 1000) String comment) {
    }

    public record HideRequest(@NotBlank @Size(max = 500) String reason) {
    }

    /** stayVerified is always false: CampusNest cannot confirm that the reviewer actually lived there. */
    public record ReviewView(Long id, String reviewerName, int rating, String comment, Instant createdAt, boolean stayVerified) {
    }

    public record ListingReviews(int count, Double average, List<ReviewView> reviews) {
    }

    public record OwnReview(Long id, int rating, String comment, boolean visible, Instant updatedAt) {
    }

    public record MyReview(boolean eligible, String reason, OwnReview review) {
    }

    public record AdminReviewView(Long id, Long listingId, String listingTitle, String reviewerName, int rating,
            String comment, String status, String hiddenReason, Instant createdAt) {
    }
}
