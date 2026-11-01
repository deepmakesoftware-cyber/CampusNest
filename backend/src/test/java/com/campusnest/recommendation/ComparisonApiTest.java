package com.campusnest.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

/** FR-4 acceptance tests: compare rent and estimated transport without presenting estimates as confirmed costs. */
class ComparisonApiTest extends ListingTestSupport {

    private static final String COMPARE = "/api/v1/student/compare";

    private MockHttpSession studentWithCollege() throws Exception {
        MockHttpSession student = registerAndLogin(Role.STUDENT);
        mockMvc.perform(put("/api/v1/student/profile").with(csrf()).session(student).contentType(MediaType.APPLICATION_JSON)
                .content("{\"collegeId\":1}")).andExpect(status().isOk());
        return student;
    }

    @Test
    void nearbyAndFarListingsShowTheCostVersusCommuteTradeOff() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        MockHttpSession admin = createAdminAndLogin();
        MockHttpSession student = studentWithCollege();
        // Near VJTI, ₹9,500 + ₹800 electricity.
        long near = publishedWithToken(owner, admin, token(), l -> { });
        // In Chembur, cheaper rent: ₹7,000 + ₹800.
        long far = publishedWithToken(owner, admin, token(), l -> {
            l.put("latitude", 19.0620);
            l.put("longitude", 72.8990);
            l.put("pincode", "400071");
            rent(l, 7000);
        });

        String body = mockMvc.perform(get(COMPARE).param("ids", near + "," + far).session(student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.daysPerMonth").value(22))
                .andExpect(jsonPath("$.collegeShortName").value("VJTI"))
                .andReturn().getResponse().getContentAsString();

        // Near: walking is free and quick, so it is suggested; total = rent + compulsory charges.
        assertThat((String) JsonPath.read(body, "$.items[0].suggestedMode")).isEqualTo("WALK");
        assertThat((Integer) JsonPath.read(body, "$.items[0].estimatedMonthlyTotal")).isEqualTo(9500 + 800);
        // No useful train journey from right next to the college.
        assertThat(option(body, 0, "LOCAL_TRAIN", "available")).isEqualTo(false);

        // Far: walking is too long; a train journey exists but has no verified pass fare yet, so it is not
        // chosen for the total; the auto-rickshaw (sourced tariff) is within the default 45-minute limit.
        assertThat(option(body, 1, "LOCAL_TRAIN", "available")).isEqualTo(true);
        assertThat(option(body, 1, "LOCAL_TRAIN", "monthlyCost")).isNull();
        assertThat((String) JsonPath.read(body, "$.items[1].suggestedMode")).isEqualTo("AUTO_RICKSHAW");
        int autoMonthly = (Integer) option(body, 1, "AUTO_RICKSHAW", "monthlyCost");
        double roadKm = ((Number) option(body, 1, "AUTO_RICKSHAW", "distanceKm")).doubleValue();
        assertThat(autoMonthly).isEqualTo((int) Math.round(27 + (roadKm - 1.5) * 18.22) * 44); // 2 trips × 22 days
        assertThat((Integer) JsonPath.read(body, "$.items[1].estimatedMonthlyTotal")).isEqualTo(7000 + 800 + autoMonthly);
    }

    @Test
    void totalIsHiddenWhenTheChosenModeHasNoKnownCost() {
        // Pure check of the suggestion rule: only a train within the limit, with unknown cost.
        var train = new com.campusnest.commute.CommuteService.CommuteOption(com.campusnest.commute.TravelMode.LOCAL_TRAIN,
                true, 30, 8, null, "Pass cost not available", "m", null);
        var walk = new com.campusnest.commute.CommuteService.CommuteOption(com.campusnest.commute.TravelMode.WALK,
                true, 120, 8, 0, "Free", "m", null);
        assertThat(ComparisonService.suggest(List.of(train, walk), 45).mode())
                .isEqualTo(com.campusnest.commute.TravelMode.LOCAL_TRAIN);
    }

    @Test
    void compareNeedsTwoToFourVisibleListingsACollegeAndAStudent() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        MockHttpSession admin = createAdminAndLogin();
        MockHttpSession student = studentWithCollege();
        long a = publishedListing(owner, admin);
        long b = publishedListing(owner, admin);
        long draft = createDraft(owner);

        mockMvc.perform(get(COMPARE).param("ids", String.valueOf(a)).session(student))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(COMPARE).param("ids", a + "," + a).session(student))
                .andExpect(status().isBadRequest()); // duplicates count once
        mockMvc.perform(get(COMPARE).param("ids", a + "," + draft).session(student))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(COMPARE).param("ids", a + "," + b).param("daysPerMonth", "40").session(student))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(COMPARE).param("ids", a + "," + b).session(owner)).andExpect(status().isForbidden());

        MockHttpSession noCollege = registerAndLogin(Role.STUDENT);
        mockMvc.perform(get(COMPARE).param("ids", a + "," + b).session(noCollege))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("COLLEGE_REQUIRED"));
    }

    @Test
    void fareRulesArePublicAndOnlyAdminsCanChangeThem() throws Exception {
        mockMvc.perform(get("/api/v1/fare-rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.mode=='AUTO_RICKSHAW')].sourceName").isNotEmpty())
                .andExpect(jsonPath("$[?(@.mode=='LOCAL_TRAIN')]").isEmpty());

        Map<String, Object> pass = Map.of("mode", "LOCAL_TRAIN", "kind", "MONTHLY_PASS_SLAB", "upToKm", 15,
                "baseFare", 200, "description", "Test pass slab", "sourceName", "Test only", "effectiveFrom", "2026-01-01");
        MockHttpSession student = registerAndLogin(Role.STUDENT);
        mockMvc.perform(post("/api/v1/admin/fare-rules").with(csrf()).session(student)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(pass)))
                .andExpect(status().isForbidden());

        MockHttpSession admin = createAdminAndLogin();
        Map<String, Object> bad = Map.of("mode", "AUTO_RICKSHAW", "kind", "PER_TRIP_METERED", "baseFare", 27,
                "description", "missing per-km", "sourceName", "x", "effectiveFrom", "2026-01-01");
        mockMvc.perform(post("/api/v1/admin/fare-rules").with(csrf()).session(admin)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_FARE_RULE"));

        String created = mockMvc.perform(post("/api/v1/admin/fare-rules").with(csrf()).session(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(pass)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long ruleId = ((Number) JsonPath.read(created, "$.id")).longValue();
        try {
            mockMvc.perform(get("/api/v1/fare-rules")).andExpect(jsonPath("$[?(@.mode=='LOCAL_TRAIN')].baseFare").isNotEmpty());
        } finally {
            // Keep the shared test database as the migration left it.
            mockMvc.perform(delete("/api/v1/admin/fare-rules/" + ruleId).with(csrf()).session(admin))
                    .andExpect(status().isNoContent());
        }
    }

    /** One field of the commute option for a given mode (a JsonPath filter always returns a list). */
    private static Object option(String body, int item, String mode, String field) {
        List<Object> values = JsonPath.read(body, "$.items[" + item + "].commuteOptions[?(@.mode=='" + mode + "')]." + field);
        return values.isEmpty() ? null : values.get(0);
    }

    @SuppressWarnings("unchecked")
    private static void rent(Map<String, Object> listing, int rent) {
        ((List<Map<String, Object>>) listing.get("rooms")).get(0).put("monthlyRent", rent);
    }
}
