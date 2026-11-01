package com.campusnest.listing;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingStatusChangeRepository extends JpaRepository<ListingStatusChange, Long> {

    List<ListingStatusChange> findByListingIdOrderByIdAsc(Long listingId);
}
