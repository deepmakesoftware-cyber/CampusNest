package com.campusnest.engagement;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;

import com.campusnest.auth.Role;
import com.campusnest.support.ListingTestSupport;
import com.jayway.jsonpath.JsonPath;

/** Phase 4: saved listings and private enquiries (FR-5 privacy for contact details). */
class EngagementTest extends ListingTestSupport {

    private static final String SAVED = "/api/v1/student/saved";

    // ---------- Saved listings ----------

    @Test
    void studentCanSaveAndUnsaveIdempotently() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        MockHttpSession admin = createAdminAndLogin();
        MockHttpSession student = registerAndLogin(Role.STUDENT);
        long id = publishedListing(owner, admin);

        mockMvc.perform(put(SAVED + "/" + id).with(csrf()).session(student)).andExpect(status().isNoContent());
        mockMvc.perform(put(SAVED + "/" + id).with(csrf()).session(student)).andExpect(status().isNoContent());
        mockMvc.perform(get(SAVED).session(student))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].listingId").value(id))
                .andExpect(jsonPath("$[0].available").value(true));

        mockMvc.perform(delete(SAVED + "/" + id).with(csrf()).session(student)).andExpect(status().isNoContent());
        mockMvc.perform(get(SAVED).session(student)).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void savedListingThatGetsSuspendedIsKeptButMarkedUnavailable() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        MockHttpSession admin = createAdminAndLogin();
        MockHttpSession student = registerAndLogin(Role.STUDENT);
        long id = publishedListing(owner, admin);
        mockMvc.perform(put(SAVED + "/" + id).with(csrf()).session(student)).andExpect(status().isNoContent());

        adminAction(admin, id, "suspend", "Under investigation").andExpect(status().isOk());

        mockMvc.perform(get(SAVED).session(student))
                .andExpect(jsonPath("$[0].available").value(false))
                .andExpect(jsonPath("$[0].status").doesNotExist());
    }

    @Test
    void cannotSaveUnpublishedListingsAndOnlyStudentsCanSave() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        MockHttpSession student = registerAndLogin(Role.STUDENT);
        long draft = createDraft(owner);

        mockMvc.perform(put(SAVED + "/" + draft).with(csrf()).session(student)).andExpect(status().isNotFound());
        mockMvc.perform(put(SAVED + "/" + draft).with(csrf()).session(owner)).andExpect(status().isForbidden());
        mockMvc.perform(get(SAVED)).andExpect(status().isUnauthorized());
    }

    // ---------- Enquiries ----------

    private ResultActions startEnquiry(MockHttpSession student, long listingId, String message, boolean share) throws Exception {
        return mockMvc.perform(post("/api/v1/student/enquiries").with(csrf()).session(student)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("listingId", listingId, "message", message, "shareContact", share))));
    }

    @Test
    void enquiryThreadWithOptInContactSharing() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        MockHttpSession admin = createAdminAndLogin();
        String studentEmail = uniqueEmail();
        register(studentEmail, Role.STUDENT);
        MockHttpSession student = login(studentEmail, PASSWORD);
        long listingId = publishedListing(owner, admin);

        String started = startEnquiry(student, listingId, "Is a double room free from June?", false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.myRole").value("STUDENT"))
                .andReturn().getResponse().getContentAsString();
        long enquiryId = ((Number) JsonPath.read(started, "$.id")).longValue();

        // Owner sees it in their inbox, waiting for a reply, WITHOUT the student's contact details.
        mockMvc.perform(get("/api/v1/owner/enquiries").session(owner))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].waitingForMyReply").value(true));
        mockMvc.perform(get("/api/v1/enquiries/" + enquiryId).session(owner))
                .andExpect(jsonPath("$.otherEmail").doesNotExist())
                .andExpect(jsonPath("$.otherPhone").doesNotExist());

        // Asking again about the same listing continues the same thread; now the student opts in.
        startEnquiry(student, listingId, "You can email me.", true)
                .andExpect(jsonPath("$.id").value(enquiryId))
                .andExpect(jsonPath("$.messages", hasSize(2)));
        mockMvc.perform(get("/api/v1/enquiries/" + enquiryId).session(owner))
                .andExpect(jsonPath("$.otherEmail").value(studentEmail));

        // Owner replies without sharing: the student still cannot see the owner's email.
        mockMvc.perform(post("/api/v1/enquiries/" + enquiryId + "/messages").with(csrf()).session(owner)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"Yes, one bed is free.\",\"shareContact\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myRole").value("OWNER"));
        mockMvc.perform(get("/api/v1/enquiries/" + enquiryId).session(student))
                .andExpect(jsonPath("$.messages", hasSize(3)))
                .andExpect(jsonPath("$.messages[2].fromMe").value(false))
                .andExpect(jsonPath("$.otherEmail").doesNotExist());
    }

    @Test
    void outsidersCannotReadOrWriteAThread() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        MockHttpSession admin = createAdminAndLogin();
        MockHttpSession student = registerAndLogin(Role.STUDENT);
        MockHttpSession otherStudent = registerAndLogin(Role.STUDENT);
        MockHttpSession otherOwner = registerAndLogin(Role.OWNER);
        long listingId = publishedListing(owner, admin);
        String started = startEnquiry(student, listingId, "Hello", false).andReturn().getResponse().getContentAsString();
        long enquiryId = ((Number) JsonPath.read(started, "$.id")).longValue();

        for (MockHttpSession outsider : new MockHttpSession[] {otherStudent, otherOwner, admin}) {
            mockMvc.perform(get("/api/v1/enquiries/" + enquiryId).session(outsider)).andExpect(status().isNotFound());
            mockMvc.perform(post("/api/v1/enquiries/" + enquiryId + "/messages").with(csrf()).session(outsider)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"spam\"}"))
                    .andExpect(status().isNotFound());
        }
        mockMvc.perform(get("/api/v1/enquiries/" + enquiryId)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/owner/enquiries").session(otherOwner)).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void enquiriesNeedAPublishedListingAndAMessage() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        MockHttpSession admin = createAdminAndLogin();
        MockHttpSession student = registerAndLogin(Role.STUDENT);
        long draft = createDraft(owner);
        long published = publishedListing(owner, admin);

        startEnquiry(student, draft, "Hello", false).andExpect(status().isNotFound());
        startEnquiry(student, published, "   ", false)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.message").exists());
        mockMvc.perform(post("/api/v1/student/enquiries").with(csrf()).session(owner)
                .contentType(MediaType.APPLICATION_JSON).content("{\"listingId\":" + published + ",\"message\":\"hi\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentsCanStartAtMostFifteenNewEnquiriesPerDay() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        MockHttpSession admin = createAdminAndLogin();
        MockHttpSession student = registerAndLogin(Role.STUDENT);
        long[] ids = new long[EnquiryService.MAX_NEW_PER_DAY + 1];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = publishedListing(owner, admin);
        }
        for (int i = 0; i < EnquiryService.MAX_NEW_PER_DAY; i++) {
            startEnquiry(student, ids[i], "Hello " + i, false).andExpect(status().isOk());
        }
        startEnquiry(student, ids[EnquiryService.MAX_NEW_PER_DAY], "One too many", false)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("TOO_MANY_ENQUIRIES"));
        // Continuing an existing thread is still allowed.
        startEnquiry(student, ids[0], "Follow-up", false).andExpect(status().isOk());
    }
}
