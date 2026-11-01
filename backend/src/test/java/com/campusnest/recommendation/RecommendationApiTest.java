package com.campusnest.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
 * FR-1 acceptance test: a student enters preferences; the backend scores REAL stored listings with the documented
 * formula, ranks them and returns a breakdown.
 */
class RecommendationApiTest extends ListingTestSupport {

    private static final String RECOMMEND = "/api/v1/student/recommendations";

    private void setCollege(MockHttpSession student) throws Exception {
        mockMvc.perform(put("/api/v1/student/profile").with(csrf()).session(student).contentType(MediaType.APPLICATION_JSON)
                .content("{\"collegeId\":1}")).andExpect(status().isOk());
    }

    /** Budget ₹11,000, 30 min, double room first, must have Wi-Fi + meals, nice to have AC. */
    private void setPreferences(MockHttpSession student) throws Exception {
        mockMvc.perform(put("/api/v1/student/preferences").with(csrf()).session(student).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"budgetMax":11000,"maxCommuteMinutes":30,"commuteMode":"ANY","roomChoices":["DOUBLE"],
                         "mustHaveAmenities":["WIFI","MEALS"],"niceToHaveAmenities":["AC"]}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomChoices[0]").value("DOUBLE"));
    }

    @Test
    void recommendationsNeedCollegeAndPreferences() throws Exception {
        MockHttpSession student = registerAndLogin(Role.STUDENT);
        mockMvc.perform(get("/api/v1/student/preferences").session(student)).andExpect(status().isNoContent());

        mockMvc.perform(get(RECOMMEND).session(student))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("COLLEGE_REQUIRED"));
        setCollege(student);
        mockMvc.perform(get(RECOMMEND).session(student))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("PREFERENCES_REQUIRED"));
    }

    @Test
    void listingsAreScoredRankedAndExplained() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        MockHttpSession admin = createAdminAndLogin();
        MockHttpSession student = registerAndLogin(Role.STUDENT);
        setCollege(student);
        setPreferences(student);

        // A: ₹9,500 + ₹800 electricity = ₹10,300 (within budget), next to VJTI.
        long a = publishedWithToken(owner, admin, token(), l -> { });
        // B: ₹13,000 + ₹800 = ₹13,800 = 25% over budget (still shown, low budget score).
        long b = publishedWithToken(owner, admin, token(), l -> rent(l, 13_000));
        // C: ₹15,000 + ₹800 = ₹15,800 = 44% over budget -> excluded.
        long c = publishedWithToken(owner, admin, token(), l -> rent(l, 15_000));

        String body = mockMvc.perform(get(RECOMMEND).session(student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collegeShortName").value("VJTI"))
                .andReturn().getResponse().getContentAsString();

        List<Integer> ids = JsonPath.read(body, "$.items[*].listingId");
        assertThat(ids).contains((int) a, (int) b).doesNotContain((int) c);
        assertThat(ids.indexOf((int) a)).isLessThan(ids.indexOf((int) b));
        Map<String, Integer> excluded = JsonPath.read(body, "$.excludedByReason");
        assertThat(excluded).containsKey("Every room is more than 30% over your budget");

        // A, worked out by hand (see docs): budget 100, commute 100 (short walk), room 100,
        // amenities 80 (Wi-Fi yes, meals yes, AC no -> 4/5), trust 56 (unverified, fresh, neutral rating, no reports).
        // Total = (3000 + 2500 + 1500 + 1200 + 840) / 100 = 90.4 -> 90
        mockMvc.perform(get(RECOMMEND + "/listings/" + a).session(student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.eligible").value(true))
                .andExpect(jsonPath("$.result.monthlyCost").value(10300))
                .andExpect(jsonPath("$.result.components[0].score").value(100))
                .andExpect(jsonPath("$.result.components[1].score").value(100))
                .andExpect(jsonPath("$.result.components[2].score").value(100))
                .andExpect(jsonPath("$.result.components[3].score").value(80))
                .andExpect(jsonPath("$.result.components[4].score").value(56))
                .andExpect(jsonPath("$.result.totalScore").value(90))
                .andExpect(jsonPath("$.result.band").value("Excellent match"))
                .andExpect(jsonPath("$.result.commute.approximate").value(true));

        // B: budget 100 × (1 − 0.2545/0.3) = 15 -> total (450 + 2500 + 1500 + 1200 + 840) / 100 = 64.9 -> 65
        mockMvc.perform(get(RECOMMEND + "/listings/" + b).session(student))
                .andExpect(jsonPath("$.result.components[0].score").value(15))
                .andExpect(jsonPath("$.result.totalScore").value(65))
                .andExpect(jsonPath("$.result.band").value("Fair match"));

        // C is explained, not silently hidden.
        mockMvc.perform(get(RECOMMEND + "/listings/" + c).session(student))
                .andExpect(jsonPath("$.result.eligible").value(false))
                .andExpect(jsonPath("$.result.exclusions[0]").value("OVER_BUDGET"));
    }

    @Test
    void onlyStudentsGetRecommendations() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        mockMvc.perform(get(RECOMMEND).session(owner)).andExpect(status().isForbidden());
        mockMvc.perform(get(RECOMMEND)).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidPreferencesAreRejected() throws Exception {
        MockHttpSession student = registerAndLogin(Role.STUDENT);
        mockMvc.perform(put("/api/v1/student/preferences").with(csrf()).session(student).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"budgetMax":50,"maxCommuteMinutes":500,"commuteMode":"ANY","roomChoices":[],
                                 "mustHaveAmenities":[],"niceToHaveAmenities":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.budgetMax").exists())
                .andExpect(jsonPath("$.fields.maxCommuteMinutes").exists());
        mockMvc.perform(put("/api/v1/student/preferences").with(csrf()).session(student).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"budgetMax":9000,"maxCommuteMinutes":30,"commuteMode":"ANY","roomChoices":[],
                                 "mustHaveAmenities":["SWIMMING_POOL"],"niceToHaveAmenities":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("UNKNOWN_AMENITY"));
    }

    @SuppressWarnings("unchecked")
    private static void rent(Map<String, Object> listing, int rent) {
        ((List<Map<String, Object>>) listing.get("rooms")).get(0).put("monthlyRent", rent);
    }
}
