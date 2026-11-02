package com.campusnest.moderation;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationRecordRepository extends JpaRepository<VerificationRecord, Long> {

    List<VerificationRecord> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
