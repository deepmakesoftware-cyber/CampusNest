package com.campusnest.roommate;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RoommateBlockRepository extends JpaRepository<RoommateBlock, Long> {

    @Query("select b from RoommateBlock b where b.blocker.id = :userId or b.blocked.id = :userId")
    List<RoommateBlock> findInvolving(Long userId);

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
}
