package com.campusnest.moderation;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusnest.auth.UserAccountRepository;
import com.campusnest.common.error.ApiException;
import com.campusnest.listing.ListingRepository;
import com.campusnest.moderation.VerificationRecord.Decision;
import com.campusnest.moderation.VerificationRecord.Method;
import com.campusnest.owner.OwnerProfile;
import com.campusnest.owner.OwnerProfileRepository;
import com.campusnest.owner.OwnerType;
import com.campusnest.owner.VerificationStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Owner identity verification (FR-3), separate from listing review:
 * the owner asks -> an administrator checks (phone/video call, visit, or looks at a document WITHOUT storing it)
 * -> VERIFIED or REJECTED, recorded with method and notes. A verification can later be REVOKED.
 * "Verified" means "CampusNest checked who this owner is", never "this property is safe".
 */
@Service
public class OwnerVerificationService {

    private final OwnerProfileRepository owners;
    private final VerificationRecordRepository records;
    private final UserAccountRepository users;
    private final ListingRepository listings;

    public OwnerVerificationService(OwnerProfileRepository owners, VerificationRecordRepository records,
            UserAccountRepository users, ListingRepository listings) {
        this.owners = owners;
        this.records = records;
        this.users = users;
        this.listings = listings;
    }

    @Transactional
    public VerificationStatus request(Long ownerId) {
        OwnerProfile p = owners.findById(ownerId)
                .orElseThrow(() -> ApiException.notFound("PROFILE_NOT_FOUND", "Owner profile not found."));
        if (p.getVerificationStatus() == VerificationStatus.PENDING || p.getVerificationStatus() == VerificationStatus.VERIFIED) {
            throw ApiException.conflict("ALREADY_REQUESTED", "Verification is already " + p.getVerificationStatus().name().toLowerCase() + ".");
        }
        p.requestVerification(Instant.now());
        return p.getVerificationStatus();
    }

    @Transactional(readOnly = true)
    public List<OwnerVerificationView> list(VerificationStatus status) {
        return owners.findAll().stream()
                .filter(p -> status == null || p.getVerificationStatus() == status)
                .sorted(Comparator.comparing(OwnerProfile::getVerificationRequestedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toView)
                .toList();
    }

    @Transactional
    public OwnerVerificationView decide(Long ownerId, Long adminId, DecisionRequest r) {
        OwnerProfile p = owners.findById(ownerId)
                .orElseThrow(() -> ApiException.notFound("OWNER_NOT_FOUND", "Owner not found."));
        VerificationStatus current = p.getVerificationStatus();
        switch (r.decision()) {
            case VERIFIED -> {
                if (current == VerificationStatus.VERIFIED) {
                    throw ApiException.conflict("INVALID_DECISION", "This owner is already verified.");
                }
                p.markVerified(users.getReferenceById(adminId), Instant.now());
            }
            case REJECTED -> {
                if (current != VerificationStatus.PENDING) {
                    throw ApiException.conflict("INVALID_DECISION", "Only pending requests can be rejected.");
                }
                p.rejectVerification();
            }
            case REVOKED -> {
                if (current != VerificationStatus.VERIFIED) {
                    throw ApiException.conflict("INVALID_DECISION", "Only verified owners can have verification revoked.");
                }
                p.revokeVerification();
            }
        }
        records.save(new VerificationRecord(p.getUser(), r.decision(), r.method(), r.notes(), users.getReferenceById(adminId)));
        return toView(p);
    }

    private OwnerVerificationView toView(OwnerProfile p) {
        long listingCount = listings.findByOwnerIdOrderByUpdatedAtDesc(p.getUserId()).size();
        List<RecordView> history = records.findByOwnerIdOrderByCreatedAtDesc(p.getUserId()).stream()
                .map(v -> new RecordView(v.getDecision(), v.getMethod(), v.getNotes(), v.getReviewer().getDisplayName(), v.getCreatedAt()))
                .toList();
        return new OwnerVerificationView(p.getUserId(), p.getUser().getDisplayName(), p.getUser().getEmail(), p.getUser().getPhone(),
                p.getOwnerType(), p.getBusinessName(), p.getVerificationStatus(), p.getVerificationRequestedAt(), listingCount, history);
    }

    // ---------------- DTOs ----------------

    public record DecisionRequest(@NotNull Decision decision, @NotNull Method method, @NotBlank @Size(max = 1000) String notes) {
    }

    public record RecordView(Decision decision, Method method, String notes, String reviewer, Instant at) {
    }

    /** For administrators only (includes contact details so the admin can call the owner). */
    public record OwnerVerificationView(Long ownerId, String displayName, String email, String phone, OwnerType ownerType,
            String businessName, VerificationStatus status, Instant requestedAt, long listingCount, List<RecordView> history) {
    }
}
