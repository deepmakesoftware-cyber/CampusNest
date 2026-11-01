package com.campusnest.listing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import com.campusnest.auth.Role;
import com.campusnest.support.ListingTestSupport;
import com.jayway.jsonpath.JsonPath;

/**
 * FR-3 acceptance tests: an owner submits a listing, it enters review, and an administrator approves,
 * rejects or requests changes. Also: ownership isolation, availability, re-review, suspension, photos.
 */
class ListingWorkflowTest extends ListingTestSupport {

    // ---------- Drafts and the submission checklist ----------

    @Test
    void newDraftShowsWhatIsMissingBeforeSubmission() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        long id = createDraft(owner);

        mockMvc.perform(get(OWNER_LISTINGS + "/" + id).session(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.ownerCanEdit").value(true))
                .andExpect(jsonPath("$.missingForSubmission").isNotEmpty())
                .andExpect(jsonPath("$.history", hasSize(1)));
    }

    @Test
    void incompleteListingCannotBeSubmitted() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        long id = createDraft(owner);
        putListing(owner, id, completeListing()).andExpect(status().isOk()); // complete, but no photo yet

        mockMvc.perform(post(OWNER_LISTINGS + "/" + id + "/submit").with(csrf()).session(owner))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("LISTING_INCOMPLETE"))
                .andExpect(jsonPath("$.fields.images").exists())
                .andExpect(jsonPath("$.fields.location").doesNotExist());
    }

    @Test
    void locationOutsideMumbaiIsNotAccepted() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        long id = createDraft(owner);
        Map<String, Object> listing = completeListing();
        listing.put("latitude", 18.5204); // Pune
        listing.put("longitude", 73.8567);
        putListing(owner, id, listing).andExpect(status().isOk());
        uploadImage(owner, id, pngBytes(), "room.png").andExpect(status().isCreated());

        mockMvc.perform(post(OWNER_LISTINGS + "/" + id + "/submit").with(csrf()).session(owner))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.location").exists());
    }

    @Test
    void roomWithMoreFreeBedsThanTotalIsRejected() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        long id = createDraft(owner);
        Map<String, Object> listing = completeListing();
        @SuppressWarnings("unchecked")
        Map<String, Object> room = ((List<Map<String, Object>>) listing.get("rooms")).get(0);
        room.put("bedsAvailable", 10);

        putListing(owner, id, listing)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields['rooms[0].bedsConsistent']").exists());
    }

    @Test
    void amenityAnswersCanBeChangedRepeatedly() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        long id = createDraft(owner);
        Map<String, Object> listing = completeListing();
        putListing(owner, id, listing).andExpect(status().isOk());
        putListing(owner, id, listing).andExpect(status().isOk()); // same answers again: no duplicate-row clash

        listing.put("amenities", List.of()); // clear all answers -> all "not specified"
        putListing(owner, id, listing)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amenities[0].available").doesNotExist());
    }

    // ---------- The full review workflow ----------

    @Test
    void submitThenApprovePublishesListingAndRecordsHistory() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        MockHttpSession admin = createAdminAndLogin();
        long id = submittedListing(owner);

        // While under review the owner cannot edit.
        putListing(owner, id, completeListing())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("LISTING_UNDER_REVIEW"));

        mockMvc.perform(get(ADMIN_LISTINGS).param("status", "PENDING_VERIFICATION").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").isNumber());

        adminAction(admin, id, "approve", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listing.status").value("PUBLISHED_AVAILABLE"))
                .andExpect(jsonPath("$.listing.publishedAt").exists())
                .andExpect(jsonPath("$.owner.verificationStatus").value("UNVERIFIED"));

        mockMvc.perform(get(OWNER_LISTINGS + "/" + id).session(owner))
                .andExpect(jsonPath("$.status").value("PUBLISHED_AVAILABLE"))
                .andExpect(jsonPath("$.history", hasSize(3)))
                .andExpect(jsonPath("$.history[2].actorRole").value("ADMIN"));

        // Photos of a published listing are public.
        long imageId = firstImageId(owner, id);
        mockMvc.perform(get("/api/v1/listing-images/" + imageId))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentType("image/png"));
    }

    @Test
    void listingWithNoFreeBedsIsPublishedAsUnavailable() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        MockHttpSession admin = createAdminAndLogin();
        long id = createDraft(owner);
        Map<String, Object> listing = completeListing();
        @SuppressWarnings("unchecked")
        Map<String, Object> room = ((List<Map<String, Object>>) listing.get("rooms")).get(0);
        room.put("bedsAvailable", 0);
        putListing(owner, id, listing).andExpect(status().isOk());
        uploadImage(owner, id, pngBytes(), "room.png").andExpect(status().isCreated());
        mockMvc.perform(post(OWNER_LISTINGS + "/" + id + "/submit").with(csrf()).session(owner)).andExpect(status().isOk());

        adminAction(admin, id, "approve", null)
                .andExpect(jsonPath("$.listing.status").value("PUBLISHED_UNAVAILABLE"));
    }

    @Test
    void requestChangesShowsReasonToOwnerWhoCanFixAndResubmit() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        MockHttpSession admin = createAdminAndLogin();
        long id = submittedListing(owner);

        adminAction(admin, id, "request-changes", "Please add a photo of the bathroom.")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listing.status").value("CHANGES_REQUESTED"));

        mockMvc.perform(get(OWNER_LISTINGS + "/" + id).session(owner))
                .andExpect(jsonPath("$.status").value("CHANGES_REQUESTED"))
                .andExpect(jsonPath("$.statusReason").value("Please add a photo of the bathroom."));

        uploadImage(owner, id, pngBytes(), "bathroom.png").andExpect(status().isCreated());
        mockMvc.perform(post(OWNER_LISTINGS + "/" + id + "/submit").with(csrf()).session(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"))
                .andExpect(jsonPath("$.statusReason").doesNotExist());
    }

    @Test
    void adminDecisionsThatAffectTheOwnerRequireAReason() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        MockHttpSession admin = createAdminAndLogin();
        long id = submittedListing(owner);

        adminAction(admin, id, "reject", " ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.reason").exists());
        adminAction(admin, id, "request-changes", "")
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectedListingIsLockedForever() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        MockHttpSession admin = createAdminAndLogin();
        long id = submittedListing(owner);

        adminAction(admin, id, "reject", "This property does not exist at the given address.")
                .andExpect(jsonPath("$.listing.status").value("REJECTED"));

        putListing(owner, id, completeListing())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("LISTING_LOCKED"));
        mockMvc.perform(post(OWNER_LISTINGS + "/" + id + "/submit").with(csrf()).session(owner))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void adminCannotSeeDraftsOrApproveTwice() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        MockHttpSession admin = createAdminAndLogin();
        long draft = createDraft(owner);
        mockMvc.perform(get(ADMIN_LISTINGS + "/" + draft).session(admin)).andExpect(status().isNotFound());
        adminAction(admin, draft, "approve", null).andExpect(status().isNotFound());

        long published = publishedListing(owner, admin);
        adminAction(admin, published, "approve", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_STATUS_TRANSITION"));
    }

    // ---------- After publication ----------

    @Test
    void availabilityUpdatesSwitchBetweenAvailableAndUnavailable() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        MockHttpSession admin = createAdminAndLogin();
        long id = publishedListing(owner, admin);
        String body = mockMvc.perform(get(OWNER_LISTINGS + "/" + id).session(owner)).andReturn().getResponse().getContentAsString();
        long roomId = ((Number) JsonPath.read(body, "$.rooms[0].id")).longValue();
        String confirmedBefore = JsonPath.read(body, "$.lastConfirmedAt");

        mockMvc.perform(patch(OWNER_LISTINGS + "/" + id + "/availability").with(csrf()).session(owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rooms\":[{\"roomId\":" + roomId + ",\"bedsAvailable\":0}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED_UNAVAILABLE"));

        String after = mockMvc.perform(patch(OWNER_LISTINGS + "/" + id + "/availability").with(csrf()).session(owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rooms\":[{\"roomId\":" + roomId + ",\"bedsAvailable\":3}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED_AVAILABLE"))
                .andReturn().getResponse().getContentAsString();
        assertThat((String) JsonPath.read(after, "$.lastConfirmedAt")).isGreaterThan(confirmedBefore);

        mockMvc.perform(patch(OWNER_LISTINGS + "/" + id + "/availability").with(csrf()).session(owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rooms\":[{\"roomId\":" + roomId + ",\"bedsAvailable\":99}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("TOO_MANY_BEDS"));
    }

    @Test
    void rentChangeStaysPublishedButTitleChangeNeedsNewReview() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        MockHttpSession admin = createAdminAndLogin();
        long id = publishedListing(owner, admin);
        String body = mockMvc.perform(get(OWNER_LISTINGS + "/" + id).session(owner)).andReturn().getResponse().getContentAsString();
        long roomId = ((Number) JsonPath.read(body, "$.rooms[0].id")).longValue();

        Map<String, Object> listing = completeListing();
        @SuppressWarnings("unchecked")
        Map<String, Object> room = ((List<Map<String, Object>>) listing.get("rooms")).get(0);
        room.put("id", roomId);
        room.put("monthlyRent", 9900);
        putListing(owner, id, listing)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED_AVAILABLE"))
                .andExpect(jsonPath("$.rooms[0].id").value(roomId))
                .andExpect(jsonPath("$.rooms[0].monthlyRent").value(9900));

        listing.put("title", "Sunrise PG Matunga (renovated)");
        putListing(owner, id, listing)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"));
    }

    @Test
    void suspendedListingHidesPhotosAndReturnsAsUnavailableWhenReinstated() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        MockHttpSession admin = createAdminAndLogin();
        long id = publishedListing(owner, admin);
        long imageId = firstImageId(owner, id);

        adminAction(admin, id, "suspend", "Multiple reports of a fake listing.")
                .andExpect(jsonPath("$.listing.status").value("SUSPENDED"));
        mockMvc.perform(get("/api/v1/listing-images/" + imageId)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/listing-images/" + imageId).session(owner)).andExpect(status().isOk());
        putListing(owner, id, completeListing()).andExpect(status().isConflict());

        adminAction(admin, id, "reinstate", null)
                .andExpect(jsonPath("$.listing.status").value("PUBLISHED_UNAVAILABLE"));
    }

    // ---------- Ownership, roles and privacy ----------

    @Test
    void ownersCannotTouchEachOthersListings() throws Exception {
        MockHttpSession ownerA = registerAndLogin(Role.OWNER);
        MockHttpSession ownerB = registerAndLogin(Role.OWNER);
        long id = createDraft(ownerA);

        mockMvc.perform(get(OWNER_LISTINGS + "/" + id).session(ownerB)).andExpect(status().isNotFound());
        putListing(ownerB, id, completeListing()).andExpect(status().isNotFound());
        mockMvc.perform(post(OWNER_LISTINGS + "/" + id + "/submit").with(csrf()).session(ownerB)).andExpect(status().isNotFound());
        mockMvc.perform(delete(OWNER_LISTINGS + "/" + id).with(csrf()).session(ownerB)).andExpect(status().isNotFound());
        uploadImage(ownerB, id, pngBytes(), "x.png").andExpect(status().isNotFound());

        // Owner B's own list does not include it.
        mockMvc.perform(get(OWNER_LISTINGS).session(ownerB)).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void studentsCannotManageListingsAndOwnersCannotModerate() throws Exception {
        MockHttpSession student = registerAndLogin(Role.STUDENT);
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        long id = submittedListing(owner);

        mockMvc.perform(get(OWNER_LISTINGS).session(student)).andExpect(status().isForbidden());
        mockMvc.perform(post(OWNER_LISTINGS).with(csrf()).session(student)
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Fake listing\"}"))
                .andExpect(status().isForbidden());
        adminAction(owner, id, "approve", null).andExpect(status().isForbidden());
        adminAction(student, id, "approve", null).andExpect(status().isForbidden());
    }

    @Test
    void draftPhotosAreVisibleOnlyToTheirOwner() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        MockHttpSession otherOwner = registerAndLogin(Role.OWNER);
        MockHttpSession admin = createAdminAndLogin();
        long id = createDraft(owner);
        uploadImage(owner, id, pngBytes(), "room.png").andExpect(status().isCreated());
        long imageId = firstImageId(owner, id);

        mockMvc.perform(get("/api/v1/listing-images/" + imageId).session(owner)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/listing-images/" + imageId)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/listing-images/" + imageId).session(otherOwner)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/listing-images/" + imageId).session(admin)).andExpect(status().isNotFound());
    }

    @Test
    void nonImageUploadIsRejected() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        long id = createDraft(owner);
        uploadImage(owner, id, "not really a picture".getBytes(StandardCharsets.UTF_8), "photo.png")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("UNSUPPORTED_IMAGE_TYPE"));
    }

    @Test
    void onlyDraftsCanBeDeleted() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        long draft = createDraft(owner);
        mockMvc.perform(delete(OWNER_LISTINGS + "/" + draft).with(csrf()).session(owner)).andExpect(status().isNoContent());
        mockMvc.perform(get(OWNER_LISTINGS + "/" + draft).session(owner)).andExpect(status().isNotFound());

        long submitted = submittedListing(owner);
        mockMvc.perform(delete(OWNER_LISTINGS + "/" + submitted).with(csrf()).session(owner))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("LISTING_NOT_DELETABLE"));
    }

    @Test
    void unknownAmenityOrRoomIdsAreRejected() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        long id = createDraft(owner);

        Map<String, Object> badAmenity = completeListing();
        badAmenity.put("amenities", List.of(Map.of("amenityId", 999999, "available", true)));
        putListing(owner, id, badAmenity).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("UNKNOWN_AMENITY"));

        Map<String, Object> badRoom = completeListing();
        @SuppressWarnings("unchecked")
        Map<String, Object> room = ((List<Map<String, Object>>) badRoom.get("rooms")).get(0);
        room.put("id", 999999);
        putListing(owner, id, badRoom).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("UNKNOWN_ROOM"));
    }
}
