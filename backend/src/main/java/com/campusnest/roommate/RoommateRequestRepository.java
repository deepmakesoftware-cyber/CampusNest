package com.campusnest.roommate;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RoommateRequestRepository extends JpaRepository<RoommateRequest, Long> {

    @Query("select r from RoommateRequest r where r.from.id = :userId or r.to.id = :userId order by r.createdAt desc")
    List<RoommateRequest> findInvolving(Long userId);

    @Query("""
            select r from RoommateRequest r
            where ((r.from.id = :a and r.to.id = :b) or (r.from.id = :b and r.to.id = :a))
              and r.status in (com.campusnest.roommate.RoommateTypes.RequestStatus.PENDING,
                               com.campusnest.roommate.RoommateTypes.RequestStatus.ACCEPTED)
            """)
    List<RoommateRequest> findActiveBetween(Long a, Long b);

    long countByFromIdAndCreatedAtAfter(Long fromId, Instant since);
}
