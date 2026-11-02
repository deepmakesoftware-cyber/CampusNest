package com.campusnest.roommate;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RoommateProfileRepository extends JpaRepository<RoommateProfile, Long> {

    /** Discoverable profiles of active accounts (suspended users are never shown). */
    @Query("""
            select p from RoommateProfile p join p.user u
            where p.discoverable = true and u.status = com.campusnest.auth.AccountStatus.ACTIVE
            """)
    List<RoommateProfile> findDiscoverable();
}
