package com.campusnest.engagement;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EnquiryRepository extends JpaRepository<Enquiry, Long> {

    Optional<Enquiry> findByListingIdAndStudentId(Long listingId, Long studentId);

    List<Enquiry> findByStudentIdOrderByLastMessageAtDesc(Long studentId);

    List<Enquiry> findByListingOwnerIdOrderByLastMessageAtDesc(Long ownerId);

    long countByStudentIdAndCreatedAtAfter(Long studentId, Instant since);
}
