package com.campusnest.owner;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusnest.auth.AppUserPrincipal;
import com.campusnest.common.error.ApiException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * The logged-in owner's own profile. Only OWNER accounts reach this controller ("/api/v1/owner/**").
 * The request record deliberately has no verificationStatus field, so an owner cannot mark themselves verified.
 */
@RestController
@RequestMapping("/api/v1/owner/profile")
public class OwnerProfileController {

    private final OwnerProfileRepository profiles;

    public OwnerProfileController(OwnerProfileRepository profiles) {
        this.profiles = profiles;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public OwnerProfileResponse get(@AuthenticationPrincipal AppUserPrincipal me) {
        return OwnerProfileResponse.from(loadOwnProfile(me));
    }

    @PutMapping
    @Transactional
    public OwnerProfileResponse update(@AuthenticationPrincipal AppUserPrincipal me,
            @Valid @RequestBody OwnerProfileRequest request) {
        OwnerProfile profile = loadOwnProfile(me);
        profile.update(request.ownerType(), request.businessName());
        return OwnerProfileResponse.from(profile);
    }

    private OwnerProfile loadOwnProfile(AppUserPrincipal me) {
        return profiles.findById(me.getId())
                .orElseThrow(() -> ApiException.notFound("PROFILE_NOT_FOUND", "Owner profile not found."));
    }

    public record OwnerProfileRequest(@NotNull OwnerType ownerType, @Size(max = 120) String businessName) {
    }

    public record OwnerProfileResponse(OwnerType ownerType, String businessName,
            VerificationStatus verificationStatus) {

        static OwnerProfileResponse from(OwnerProfile p) {
            return new OwnerProfileResponse(p.getOwnerType(), p.getBusinessName(), p.getVerificationStatus());
        }
    }
}
