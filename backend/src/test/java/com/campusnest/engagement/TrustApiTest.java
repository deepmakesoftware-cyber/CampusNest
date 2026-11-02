package com.campusnest.engagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;

import com.campusnest.auth.Role;
import com.campusnest.support.ListingTestSupport;
import com.jayway.jsonpath.JsonPath;

/** FR-3 (trust): reviews, reports, owner verification, and their effect on the suitability score. */
class TrustApiTest extends ListingTestSupport {

    private ResultActions enquire(MockHttpSession student, long listingId) throws Exception {
        return mockMvc.perform(post("/api/v1/student/enquiries").with(csrf()).session(student).contentType(MediaType.APPLICATION_JSON)
                .content("{\"listingId\":" + listingId + ",\"message\":\"Is it available?\"}"));
    }

    private ResultActions review(MockHttpSession student, long listingId, int rating, String comment) throws Exception {
        return mockMvc.perform(put("/api/v1/student/listings/" + listingId + "/review").with(csrf()).session(student)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(Map.of("rating", rating, "comment", comment))));
    }

    private ResultActions report(MockHttpSession who, String type, long targetId, String reason) throws Exception {
        return mockMvc.perform(post("/api/v1/reports").with(csrf()).session(who).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("targetType", type, "targetId", targetId, "reason", reason, "details", "Test"))));
    }

    private MockHttpSession studentNamed(String name) throws Exception {
        MockHttpSession s = registerAndLogin(Role.STUDENT);
        mockMvc.perform(put("/api/v1/users/me").with(csrf()).session(s).contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"" + name + "\"}")).andExpect(status().isOk());
        return s;
    }

    // ---------------- Reviews ----------------

    @Test
    void onlyStudentsWhoEnquiredCanReviewAndReviewsAreLabelledUnverified() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        MockHttpSession admin = createAdminAndLogin();
        MockHttpSession student = studentNamed("Ravi Kumar");
        long id = publishedListing(owner, admin);

        mockMvc.perform(get("/api/v1/student/listings/" + id + "/review").session(student))
                .andExpect(jsonPath("$.eligible").value(false));
        review(student, id, 5, "Great").andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("REVIEW_NOT_ALLOWED"));

        enquire(student, id).andExpect(status().isOk());
        review(student, id, 4, "Clean rooms, helpful owner.").andExpect(status().isOk());
        review(student, id, 5, "Even better after a month.").andExpect(status().isOk()); // edits, not a second review

        mockMvc.perform(get("/api/v1/listings/" + id + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.average").value(5.0))
                .andExpect(jsonPath("$.reviews[0].reviewerName").value("Ravi K."))
                .andExpect(jsonPath("$.reviews[0].stayVerified").value(false));

        review(student, id, 9, "x").andExpect(status().isBadRequest());
        review(owner, id, 5, "My own place is great").andExpect(status().isForbidden());
    }

    @Test
    void hiddenReviewsDisappearAndEditingDoesNotUnhideThem() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        MockHttpSession admin = createAdminAndLogin();
        MockHttpSession student = registerAndLogin(Role.STUDENT);
        long id = publishedListing(owner, admin);
        enquire(student, id);
        long reviewId = ((Number) JsonPath.read(review(student, id, 1, "Owner is a cheat, call 9999999999")
                .andReturn().getResponse().getContentAsString(), "$.id")).longValue();

        mockMvc.perform(post("/api/v1/admin/reviews/" + reviewId + "/hide").with(csrf()).session(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Contains a phone number\"}"))
                .andExpect(jsonPath("$.status").value("HIDDEN"));
        mockMvc.perform(get("/api/v1/listings/" + id + "/reviews")).andExpect(jsonPath("$.count").value(0));

        review(student, id, 2, "Edited").andExpect(jsonPath("$.visible").value(false));
        mockMvc.perform(get("/api/v1/listings/" + id + "/reviews")).andExpect(jsonPath("$.count").value(0));
    }

    // ---------------- Trust score ----------------

    @Test
    void reviewsAndUpheldReportsChangeTheTrustPartOfTheScore() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        MockHttpSession admin = createAdminAndLogin();
        MockHttpSession student = registerAndLogin(Role.STUDENT);
        mockMvc.perform(put("/api/v1/student/profile").with(csrf()).session(student).contentType(MediaType.APPLICATION_JSON)
                .content("{\"collegeId\":1}"));
        mockMvc.perform(put("/api/v1/student/preferences").with(csrf()).session(student).contentType(MediaType.APPLICATION_JSON)
                .content("{\"budgetMax\":11000,\"maxCommuteMinutes\":30,\"commuteMode\":\"ANY\",\"roomChoices\":[],\"mustHaveAmenities\":[],\"niceToHaveAmenities\":[]}"))
                .andExpect(status().isOk());
        long id = publishedListing(owner, admin);
        String url = "/api/v1/student/recommendations/listings/" + id;

        // Before: unverified 0 + fresh 30 + neutral rating 16 + no reports 10 = 56
        mockMvc.perform(get(url).session(student)).andExpect(jsonPath("$.result.components[4].score").value(56));

        enquire(student, id);
        review(student, id, 5, "Excellent");
        // One 5★ review: (3 × 3.5 + 5) / 4 = 3.875 -> 25 × 2.875 / 4 = 18 -> 58
        mockMvc.perform(get(url).session(student)).andExpect(jsonPath("$.result.components[4].score").value(58));

        String created = report(student, "LISTING", id, "WRONG_INFORMATION").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long reportId = ((Number) JsonPath.read(created, "$.id")).longValue();
        mockMvc.perform(post("/api/v1/admin/reports/" + reportId + "/resolve").with(csrf()).session(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"outcome\":\"UPHELD\",\"note\":\"Rent shown is lower than charged.\"}"))
                .andExpect(jsonPath("$.status").value("UPHELD"));
        // Upheld report in the last 90 days: loses the 10 "no reports" points -> 48
        mockMvc.perform(get(url).session(student))
                .andExpect(jsonPath("$.result.components[4].score").value(48))
                .andExpect(jsonPath("$.result.components[4].explanation").value(org.hamcrest.Matchers.containsString("1 review")));
    }

    // ---------------- Reports ----------------

    @Test
    void reportsAreValidatedDeduplicatedAndResolvedOnceByAdmins() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        MockHttpSession admin = createAdminAndLogin();
        MockHttpSession student = registerAndLogin(Role.STUDENT);
        long id = publishedListing(owner, admin);
        long draft = createDraft(owner);
        long studentId = users.findAll().stream().filter(u -> u.getRole() == Role.STUDENT)
                .max((a, b) -> Long.compare(a.getId(), b.getId())).orElseThrow().getId();
        long adminId = users.findAll().stream().filter(u -> u.getRole() == Role.ADMIN).findFirst().orElseThrow().getId();

        long reportId = ((Number) JsonPath.read(report(student, "LISTING", id, "FAKE_LISTING").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id")).longValue();
        report(student, "LISTING", id, "SPAM").andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ALREADY_REPORTED"));
        report(student, "LISTING", draft, "SPAM").andExpect(status().isNotFound());
        report(student, "USER", studentId, "SPAM").andExpect(status().isBadRequest());
        report(student, "USER", adminId, "SPAM").andExpect(status().isNotFound());
        report(admin, "LISTING", id, "SPAM").andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/reports").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/admin/reports").param("status", "OPEN").session(student)).andExpect(status().isForbidden());
        String queue = mockMvc.perform(get("/api/v1/admin/reports").param("status", "OPEN").session(admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Integer> openIds = JsonPath.read(queue, "$.items[*].id");
        assertThat(openIds).contains((int) reportId);

        mockMvc.perform(post("/api/v1/admin/reports/" + reportId + "/resolve").with(csrf()).session(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"outcome\":\"DISMISSED\",\"note\":\"Visited; listing is genuine.\"}"))
                .andExpect(jsonPath("$.status").value("DISMISSED"))
                .andExpect(jsonPath("$.targetSummary").value(org.hamcrest.Matchers.startsWith("Listing: ")));
        mockMvc.perform(post("/api/v1/admin/reports/" + reportId + "/resolve").with(csrf()).session(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"outcome\":\"UPHELD\",\"note\":\"again\"}"))
                .andExpect(status().isConflict());
        // After resolution the student may report again if something new happens.
        report(student, "LISTING", id, "UNSAFE").andExpect(status().isCreated());
    }

    // ---------------- Owner verification ----------------

    @Test
    void ownerVerificationIsRequestedDecidedRecordedAndShownPublicly() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        MockHttpSession admin = createAdminAndLogin();
        long listingId = publishedListing(owner, admin);
        long ownerId = users.findAll().stream().filter(u -> u.getRole() == Role.OWNER)
                .max((a, b) -> Long.compare(a.getId(), b.getId())).orElseThrow().getId();
        String decide = "/api/v1/admin/owners/" + ownerId + "/verification";

        mockMvc.perform(post(decide).with(csrf()).session(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"REJECTED\",\"method\":\"PHONE_CALL\",\"notes\":\"x\"}"))
                .andExpect(status().isConflict()); // nothing pending yet

        mockMvc.perform(post("/api/v1/owner/verification-request").with(csrf()).session(owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$").value("PENDING"));
        mockMvc.perform(post("/api/v1/owner/verification-request").with(csrf()).session(owner))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/admin/owners").param("status", "PENDING").session(admin))
                .andExpect(jsonPath("$[?(@.ownerId == " + ownerId + ")]").isNotEmpty());
        mockMvc.perform(post(decide).with(csrf()).session(owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"VERIFIED\",\"method\":\"PHONE_CALL\",\"notes\":\"self\"}")).andExpect(status().isForbidden());
        mockMvc.perform(post(decide).with(csrf()).session(admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"VERIFIED\",\"method\":\"PHONE_CALL\",\"notes\":\"\"}")).andExpect(status().isBadRequest());

        mockMvc.perform(post(decide).with(csrf()).session(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"VERIFIED\",\"method\":\"VIDEO_CALL\",\"notes\":\"Matched name and property on video call.\"}"))
                .andExpect(jsonPath("$.status").value("VERIFIED"))
                .andExpect(jsonPath("$.history", hasSize(1)))
                .andExpect(jsonPath("$.history[0].method").value("VIDEO_CALL"));
        mockMvc.perform(get("/api/v1/listings/" + listingId)).andExpect(jsonPath("$.owner.verificationStatus").value("VERIFIED"));

        mockMvc.perform(post(decide).with(csrf()).session(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"REVOKED\",\"method\":\"OTHER\",\"notes\":\"Upheld fraud reports.\"}"))
                .andExpect(jsonPath("$.status").value("UNVERIFIED"))
                .andExpect(jsonPath("$.history", hasSize(2)));
    }

    @Test
    void adminOverviewCountsWhatNeedsAttention() throws Exception {
        MockHttpSession admin = createAdminAndLogin();
        mockMvc.perform(get("/api/v1/admin/overview").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listingsAwaitingReview").isNumber())
                .andExpect(jsonPath("$.openReports").isNumber())
                .andExpect(jsonPath("$.ownerVerificationRequests").isNumber());
        mockMvc.perform(get("/api/v1/admin/overview").session(registerAndLogin(Role.STUDENT))).andExpect(status().isForbidden());
    }
}
