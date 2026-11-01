package com.campusnest.moderation;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campusnest.auth.AppUserPrincipal;
import com.campusnest.common.web.PageResponse;
import com.campusnest.listing.ListingDtos.AdminListingDetail;
import com.campusnest.listing.ListingDtos.AdminListingSummary;
import com.campusnest.listing.ListingDtos.ReasonRequest;
import com.campusnest.listing.ListingStatus;

import jakarta.validation.Valid;

/** Listing review actions. Only ADMIN accounts reach these URLs ("/api/v1/admin/**"). */
@RestController
@RequestMapping("/api/v1/admin/listings")
public class AdminListingController {

    private final AdminListingService service;

    public AdminListingController(AdminListingService service) {
        this.service = service;
    }

    /** GET /api/v1/admin/listings?status=PENDING_VERIFICATION&page=0 (status optional = all except drafts). */
    @GetMapping
    public PageResponse<AdminListingSummary> list(@RequestParam(required = false) ListingStatus status,
            @RequestParam(defaultValue = "0") int page) {
        return service.list(status, page);
    }

    @GetMapping("/{id}")
    public AdminListingDetail get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/approve")
    public AdminListingDetail approve(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id) {
        return service.approve(id, me.getId());
    }

    @PostMapping("/{id}/request-changes")
    public AdminListingDetail requestChanges(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id,
            @Valid @RequestBody ReasonRequest request) {
        return service.requestChanges(id, me.getId(), request.reason());
    }

    @PostMapping("/{id}/reject")
    public AdminListingDetail reject(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id,
            @Valid @RequestBody ReasonRequest request) {
        return service.reject(id, me.getId(), request.reason());
    }

    @PostMapping("/{id}/suspend")
    public AdminListingDetail suspend(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id,
            @Valid @RequestBody ReasonRequest request) {
        return service.suspend(id, me.getId(), request.reason());
    }

    @PostMapping("/{id}/reinstate")
    public AdminListingDetail reinstate(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id) {
        return service.reinstate(id, me.getId());
    }
}
