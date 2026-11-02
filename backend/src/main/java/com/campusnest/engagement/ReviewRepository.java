package com.campusnest.engagement;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByListingIdAndStudentId(Long listingId, Long studentId);

    List<Review> findByListingIdAndStatusOrderByCreatedAtDesc(Long listingId, String status);

    /** [count, sum of ratings] of visible reviews for one listing, used by the trust score. */
    @Query("select count(r), coalesce(sum(r.rating), 0) from Review r where r.listing.id = :listingId and r.status = 'VISIBLE'")
    List<Object[]> visibleStats(Long listingId);
}
