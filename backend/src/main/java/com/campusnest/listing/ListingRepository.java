package com.campusnest.listing;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    List<Listing> findByOwnerIdOrderByUpdatedAtDesc(Long ownerId);

    /** Loads a listing only if it belongs to this owner: the core ownership (anti-IDOR) check. */
    Optional<Listing> findByIdAndOwnerId(Long id, Long ownerId);

    Page<Listing> findByStatus(ListingStatus status, Pageable pageable);

    Page<Listing> findByStatusNot(ListingStatus status, Pageable pageable);

    /** Listings students may see: published, and the owner's account is not suspended. */
    @Query("""
            select l from Listing l join l.owner o
            where l.status in (com.campusnest.listing.ListingStatus.PUBLISHED_AVAILABLE,
                               com.campusnest.listing.ListingStatus.PUBLISHED_UNAVAILABLE)
              and o.status = com.campusnest.auth.AccountStatus.ACTIVE
            """)
    List<Listing> findVisibleToStudents();

    boolean existsBySampleDataTrue();
}
