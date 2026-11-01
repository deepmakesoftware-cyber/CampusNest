package com.campusnest.engagement;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedListingRepository extends JpaRepository<SavedListing, Long> {

    List<SavedListing> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    Optional<SavedListing> findByStudentIdAndListingId(Long studentId, Long listingId);
}
