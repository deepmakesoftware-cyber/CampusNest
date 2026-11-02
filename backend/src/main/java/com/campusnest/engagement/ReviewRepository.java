package com.campusnest.engagement;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByListingIdAndStudentId(Long listingId, Long studentId);

    List<Review> findByListingIdAndStatusOrderByCreatedAtDesc(Long listingId, String status);

    /** [listingId, count, sum of ratings] for every listing with visible reviews: one query for a whole ranking. */
    @Query("select r.listing.id, count(r), sum(r.rating) from Review r where r.status = 'VISIBLE' group by r.listing.id")
    List<Object[]> visibleStatsByListing();
}
