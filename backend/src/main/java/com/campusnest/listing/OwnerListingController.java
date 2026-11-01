package com.campusnest.listing;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.campusnest.auth.AppUserPrincipal;
import com.campusnest.listing.ListingDtos.AvailabilityRequest;
import com.campusnest.listing.ListingDtos.CreateListingRequest;
import com.campusnest.listing.ListingDtos.ListingDetail;
import com.campusnest.listing.ListingDtos.ListingRequest;
import com.campusnest.listing.ListingDtos.ListingSummary;

import jakarta.validation.Valid;

/** Owner listing management. Only OWNER accounts reach these URLs ("/api/v1/owner/**" in SecurityConfig). */
@RestController
@RequestMapping("/api/v1/owner/listings")
public class OwnerListingController {

    private final OwnerListingService service;

    public OwnerListingController(OwnerListingService service) {
        this.service = service;
    }

    @GetMapping
    public List<ListingSummary> list(@AuthenticationPrincipal AppUserPrincipal me) {
        return service.list(me.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ListingDetail create(@AuthenticationPrincipal AppUserPrincipal me, @Valid @RequestBody CreateListingRequest request) {
        return service.create(me.getId(), request.title());
    }

    @GetMapping("/{id}")
    public ListingDetail get(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id) {
        return service.get(me.getId(), id);
    }

    @PutMapping("/{id}")
    public ListingDetail update(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id,
            @Valid @RequestBody ListingRequest request) {
        return service.update(me.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id) {
        service.delete(me.getId(), id);
    }

    @PostMapping("/{id}/submit")
    public ListingDetail submit(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id) {
        return service.submit(me.getId(), id);
    }

    @PatchMapping("/{id}/availability")
    public ListingDetail updateAvailability(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id,
            @Valid @RequestBody AvailabilityRequest request) {
        return service.updateAvailability(me.getId(), id, request);
    }

    @PostMapping(path = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ListingDetail addImage(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        return service.addImage(me.getId(), id, file.getBytes());
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ListingDetail deleteImage(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id,
            @PathVariable Long imageId) {
        return service.deleteImage(me.getId(), id, imageId);
    }
}
