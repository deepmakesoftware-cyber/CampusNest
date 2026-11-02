package com.campusnest;

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

import com.campusnest.auth.Role;
import com.campusnest.support.ListingTestSupport;
import com.jayway.jsonpath.JsonPath;

/**
 * Phase 9: the whole product in one story, through the real API and database, in the order of the faculty
 * demonstration requirements (master prompt section 12). If this passes, the demo flow works end to end.
 */
class FullJourneyTest extends ListingTestSupport {

    private MockHttpSession studentWithProfile(String name, String gender) throws Exception {
        MockHttpSession s = registerAndLogin(Role.STUDENT);
        mockMvc.perform(put("/api/v1/users/me").with(csrf()).session(s).contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"" + name + "\"}")).andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/student/profile").with(csrf()).session(s).contentType(MediaType.APPLICATION_JSON)
                .content("{\"collegeId\":1,\"course\":\"MCA\",\"yearOfStudy\":1,\"gender\":\"" + gender + "\"}")).andExpect(status().isOk());
        return s;
    }

    private void roommateProfile(MockHttpSession s) throws Exception {
        mockMvc.perform(put("/api/v1/student/roommates/profile").with(csrf()).session(s).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"discoverable":true,"budgetMin":8000,"budgetMax":11000,"preferredLocalities":["Matunga"],
                         "anywhereNearCollege":false,"sleepSchedule":"EARLY","smoking":"NON_SMOKER","cannotLiveWithSmoker":true,
                         "studyEnvironment":"QUIET","cleanliness":4,"guests":2,"foodPreference":"VEG","sharingSize":"EITHER",
                         "sameGenderOnly":true,"about":"Journey test"}
                        """)).andExpect(status().isOk());
    }

    @Test
    void theDemonstrationJourneyWorksEndToEnd() throws Exception {
        MockHttpSession admin = createAdminAndLogin();

        // 1. A student registers and enters preferences.
        MockHttpSession student = studentWithProfile("Neha Verma", "FEMALE");
        mockMvc.perform(put("/api/v1/student/preferences").with(csrf()).session(student).contentType(MediaType.APPLICATION_JSON)
                .content("{\"budgetMax\":11000,\"maxCommuteMinutes\":40,\"commuteMode\":\"ANY\",\"roomChoices\":[\"DOUBLE\"],"
                        + "\"mustHaveAmenities\":[\"WIFI\"],\"niceToHaveAmenities\":[\"AC\"]}"))
                .andExpect(status().isOk());

        // 7. The owner listing workflow, and 8. the administrator verification workflow.
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        String token = token();
        long listing = createDraft(owner);
        Map<String, Object> body = completeListing();
        body.put("title", "Journey PG " + token);
        putListing(owner, listing, body).andExpect(status().isOk());
        uploadImage(owner, listing, pngBytes(), "room.png").andExpect(status().isCreated());
        mockMvc.perform(post(OWNER_LISTINGS + "/" + listing + "/submit").with(csrf()).session(owner)).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/listings").param("q", token)).andExpect(jsonPath("$.items", hasSize(0))); // not yet public
        adminAction(admin, listing, "request-changes", "Please add the landmark to the address.").andExpect(status().isOk());
        body.put("addressLine", "12, Example Lane, opposite Five Gardens");
        putListing(owner, listing, body).andExpect(status().isOk());
        mockMvc.perform(post(OWNER_LISTINGS + "/" + listing + "/submit").with(csrf()).session(owner)).andExpect(status().isOk());
        adminAction(admin, listing, "approve", null).andExpect(jsonPath("$.listing.status").value("PUBLISHED_AVAILABLE"));

        long ownerId = users.findAll().stream().filter(u -> u.getRole() == Role.OWNER)
                .max((a, b) -> Long.compare(a.getId(), b.getId())).orElseThrow().getId();
        mockMvc.perform(post("/api/v1/owner/verification-request").with(csrf()).session(owner)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/admin/owners/" + ownerId + "/verification").with(csrf()).session(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"VERIFIED\",\"method\":\"PHONE_CALL\",\"notes\":\"Confirmed by phone.\"}")).andExpect(status().isOk());

        // A second, cheaper listing further away (for the comparison).
        long farListing = publishedWithToken(owner, admin, token(), l -> {
            l.put("latitude", 19.0620);
            l.put("longitude", 72.8990);
            l.put("pincode", "400071");
        });

        // 2. Multiple accommodation options are displayed; 3. scores are calculated; 4. recommendations are ranked.
        mockMvc.perform(get("/api/v1/listings").param("q", token).param("collegeId", "1"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].ownerVerification").value("VERIFIED"));
        String recs = mockMvc.perform(get("/api/v1/student/recommendations").session(student))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Integer> scores = JsonPath.read(recs, "$.items[*].result.totalScore");
        assertThat(scores).isNotEmpty().isSortedAccordingTo((a, b) -> Integer.compare(b, a)); // ranked, best first
        List<Integer> ranked = JsonPath.read(recs, "$.items[*].listingId");
        assertThat(ranked).contains((int) listing);
        mockMvc.perform(get("/api/v1/student/recommendations/listings/" + listing).session(student))
                .andExpect(jsonPath("$.result.components", hasSize(5)))
                .andExpect(jsonPath("$.result.components[4].explanation").value(org.hamcrest.Matchers.startsWith("Owner identity checked")));

        // 5. Cost-versus-commute comparison.
        mockMvc.perform(get("/api/v1/student/compare").param("ids", listing + "," + farListing).session(student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].estimatedMonthlyTotal").isNumber());

        // Enquiry and review (trust).
        mockMvc.perform(post("/api/v1/student/enquiries").with(csrf()).session(student).contentType(MediaType.APPLICATION_JSON)
                .content("{\"listingId\":" + listing + ",\"message\":\"Is a double free from June?\"}")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/owner/enquiries").session(owner)).andExpect(jsonPath("$[0].otherName").value("Neha Verma"));
        mockMvc.perform(put("/api/v1/student/listings/" + listing + "/review").with(csrf()).session(student)
                .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":4,\"comment\":\"Helpful owner.\"}")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/listings/" + listing + "/reviews")).andExpect(jsonPath("$.count").value(1));

        // 6. At least one roommate match with a compatibility score.
        MockHttpSession roommate = studentWithProfile("Sana Shaikh", "FEMALE");
        roommateProfile(student);
        roommateProfile(roommate);
        String matches = mockMvc.perform(get("/api/v1/student/roommates/matches").session(student))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> sana = JsonPath.read(matches, "$[?(@.person.shortName == 'Sana S.')]");
        assertThat(sana).isNotEmpty();
        List<Integer> percent = JsonPath.read(matches, "$[?(@.person.shortName == 'Sana S.')].compatibility.percent");
        assertThat(percent).containsExactly(100);

        // 9. Privacy and role-based access, checked by calling the API directly.
        assertThat(matches).doesNotContain("Shaikh");
        adminAction(student, listing, "suspend", "trying").andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/student/recommendations").session(owner)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(OWNER_LISTINGS + "/" + listing).session(registerAndLogin(Role.OWNER))).andExpect(status().isNotFound());
    }
}
