package com.campusnest.moderation;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campusnest.auth.AppUserPrincipal;
import com.campusnest.engagement.Report;
import com.campusnest.engagement.ReportRepository;
import com.campusnest.listing.ListingRepository;
import com.campusnest.listing.ListingStatus;
import com.campusnest.moderation.OwnerVerificationService.DecisionRequest;
import com.campusnest.moderation.OwnerVerificationService.OwnerVerificationView;
import com.campusnest.owner.OwnerProfileRepository;
import com.campusnest.owner.VerificationStatus;

import jakarta.validation.Valid;

/** Owner verification (owner side + admin side) and the admin's "what needs attention" overview. */
@RestController
public class ModerationController {

    private final OwnerVerificationService verification;
    private final ListingRepository listings;
    private final ReportRepository reports;
    private final OwnerProfileRepository owners;

    public ModerationController(OwnerVerificationService verification, ListingRepository listings, ReportRepository reports,
            OwnerProfileRepository owners) {
        this.verification = verification;
        this.listings = listings;
        this.reports = reports;
        this.owners = owners;
    }

    @PostMapping("/api/v1/owner/verification-request")
    public VerificationStatus request(@AuthenticationPrincipal AppUserPrincipal me) {
        return verification.request(me.getId());
    }

    @GetMapping("/api/v1/admin/owners")
    public List<OwnerVerificationView> owners(@RequestParam(required = false) VerificationStatus status) {
        return verification.list(status);
    }

    @PostMapping("/api/v1/admin/owners/{ownerId}/verification")
    public OwnerVerificationView decide(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long ownerId,
            @Valid @RequestBody DecisionRequest request) {
        return verification.decide(ownerId, me.getId(), request);
    }

    @GetMapping("/api/v1/admin/overview")
    @Transactional(readOnly = true)
    public Overview overview() {
        return new Overview(
                listings.findByStatus(ListingStatus.PENDING_VERIFICATION, Pageable.unpaged()).getTotalElements(),
                reports.countByStatus(Report.Status.OPEN),
                owners.findAll().stream().filter(o -> o.getVerificationStatus() == VerificationStatus.PENDING).count());
    }

    public record Overview(long listingsAwaitingReview, long openReports, long ownerVerificationRequests) {
    }
}
