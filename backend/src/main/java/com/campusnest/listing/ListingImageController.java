package com.campusnest.listing;

import java.time.Duration;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.campusnest.auth.AppUserPrincipal;
import com.campusnest.auth.Role;
import com.campusnest.common.error.ApiException;
import com.campusnest.common.storage.ImageStorage;

/**
 * Serves listing photos. Photos of PUBLISHED listings are public. Photos of unpublished listings are visible
 * only to the listing's owner and to administrators (never drafts for admins). Everyone else gets 404, so the
 * existence of unpublished listings is not revealed.
 */
@RestController
public class ListingImageController {

    private final ListingImageRepository images;
    private final ImageStorage storage;

    public ListingImageController(ListingImageRepository images, ImageStorage storage) {
        this.images = images;
        this.storage = storage;
    }

    @GetMapping("/api/v1/listing-images/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> get(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal me) {
        ListingImage image = images.findById(id).orElseThrow(ListingImageController::notFound);
        Listing listing = image.getListing();
        ListingStatus status = listing.getStatus();

        boolean isPublic = PublicListingService.isVisible(listing);
        boolean isOwner = me != null && listing.getOwner().getId().equals(me.getId());
        boolean isReviewingAdmin = me != null && me.getRole() == Role.ADMIN && status != ListingStatus.DRAFT;
        if (!isPublic && !isOwner && !isReviewingAdmin) {
            throw notFound();
        }

        CacheControl cache = isPublic
                ? CacheControl.maxAge(Duration.ofDays(1)).cachePublic()
                : CacheControl.noStore();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .cacheControl(cache)
                .body(storage.read(image.getStorageKey()));
    }

    private static ApiException notFound() {
        return ApiException.notFound("IMAGE_NOT_FOUND", "Photo not found.");
    }
}
