package com.campusnest.engagement;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.campusnest.auth.AppUserPrincipal;
import com.campusnest.auth.Role;
import com.campusnest.common.error.ApiException;
import com.campusnest.common.web.PageResponse;
import com.campusnest.engagement.ReportService.CreateReport;
import com.campusnest.engagement.ReportService.ReportView;
import com.campusnest.engagement.ReportService.ResolveReport;

import jakarta.validation.Valid;

@RestController
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    /** Any logged-in user (student or owner). Admins resolve reports rather than file them. */
    @PostMapping("/api/v1/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportView create(@AuthenticationPrincipal AppUserPrincipal me, @Valid @RequestBody CreateReport request) {
        if (me.getRole() == Role.ADMIN) {
            throw ApiException.badRequest("INVALID_REPORT", "Administrators act on reports instead of filing them.");
        }
        return service.create(me.getId(), request);
    }

    @GetMapping("/api/v1/admin/reports")
    public PageResponse<ReportView> list(@RequestParam(required = false) Report.Status status,
            @RequestParam(defaultValue = "0") int page) {
        return service.list(status, page);
    }

    @PostMapping("/api/v1/admin/reports/{id}/resolve")
    public ReportView resolve(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id,
            @Valid @RequestBody ResolveReport request) {
        return service.resolve(id, me.getId(), request);
    }
}
