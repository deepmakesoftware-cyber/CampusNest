package com.campusnest.engagement;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.campusnest.auth.AppUserPrincipal;
import com.campusnest.engagement.EnquiryService.EnquirySummary;
import com.campusnest.engagement.EnquiryService.EnquiryView;
import com.campusnest.engagement.EnquiryService.ReplyRequest;
import com.campusnest.engagement.EnquiryService.StartEnquiryRequest;

import jakarta.validation.Valid;

/**
 * Enquiry endpoints. Starting and listing are role areas (/student, /owner). Reading and replying to a thread
 * (/api/v1/enquiries/{id}) needs a login, and the service checks the caller is one of the two participants.
 */
@RestController
public class EnquiryController {

    private final EnquiryService service;

    public EnquiryController(EnquiryService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/student/enquiries")
    public EnquiryView start(@AuthenticationPrincipal AppUserPrincipal me, @Valid @RequestBody StartEnquiryRequest request) {
        return service.start(me.getId(), request);
    }

    @GetMapping("/api/v1/student/enquiries")
    public List<EnquirySummary> studentInbox(@AuthenticationPrincipal AppUserPrincipal me) {
        return service.listForStudent(me.getId());
    }

    @GetMapping("/api/v1/owner/enquiries")
    public List<EnquirySummary> ownerInbox(@AuthenticationPrincipal AppUserPrincipal me) {
        return service.listForOwner(me.getId());
    }

    @GetMapping("/api/v1/enquiries/{id}")
    public EnquiryView get(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id) {
        return service.get(id, me.getId());
    }

    @PostMapping("/api/v1/enquiries/{id}/messages")
    public EnquiryView reply(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id,
            @Valid @RequestBody ReplyRequest request) {
        return service.reply(id, me.getId(), request);
    }
}
