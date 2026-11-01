package com.campusnest.listing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import com.campusnest.auth.Role;
import com.campusnest.support.ListingTestSupport;
import com.jayway.jsonpath.JsonPath;

/** Phase 4: what students and visitors can find and see. Results must come from real, published listings only. */
class PublicDiscoveryTest extends ListingTestSupport {

    private static final String SEARCH = "/api/v1/listings";

    @Test
    void searchShowsOnlyPublishedListings() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        MockHttpSession admin = createAdminAndLogin();
        String token = token();
        long published = publishedWithToken(owner, admin, token, l -> { });

        // A draft and a submitted listing with the same token must NOT appear.
        long draft = createDraft(owner);
        Map<String, Object> other = completeListing();
        other.put("title", "Draft " + token);
        putListing(owner, draft, other);

        mockMvc.perform(get(SEARCH).param("q", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id").value(published))
                .andExpect(jsonPath("$.items[0].fromRent").value(9500))
                .andExpect(jsonPath("$.items[0].coverImageUrl").exists());

        mockMvc.perform(get(SEARCH + "/" + draft)).andExpect(status().isNotFound());
    }

    @Test
    void listingsOfSuspendedOwnersDisappear() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        MockHttpSession admin = createAdminAndLogin();
        String token = token();
        long id = publishedWithToken(owner, admin, token, l -> { });
        long ownerId = users.findAll().stream().filter(u -> u.getRole() == Role.OWNER)
                .max((a, b) -> Long.compare(a.getId(), b.getId())).orElseThrow().getId();

        mockMvc.perform(post("/api/v1/admin/users/" + ownerId + "/suspend").with(csrf()).session(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Fraud reports\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get(SEARCH).param("q", token)).andExpect(jsonPath("$.items", hasSize(0)));
        mockMvc.perform(get(SEARCH + "/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void publicDetailNeverExposesOwnerContactDetails() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        MockHttpSession admin = createAdminAndLogin();
        mockMvc.perform(put("/api/v1/users/me").with(csrf()).session(owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"Meera Owner\",\"phone\":\"+91 90000 11111\"}")).andExpect(status().isOk());
        long id = publishedWithToken(owner, admin, token(), l -> { });

        String body = mockMvc.perform(get(SEARCH + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner.displayName").value("Meera Owner"))
                .andExpect(jsonPath("$.owner.verificationStatus").value("UNVERIFIED"))
                .andExpect(jsonPath("$.freshness").value("FRESH"))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("90000").doesNotContain("@example.com").doesNotContain("missingForSubmission");
    }

    @Test
    void filtersNarrowResultsCorrectly() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        MockHttpSession admin = createAdminAndLogin();
        String token = token();
        // Women-only, double room at ₹9,500, Wi-Fi yes, AC no (see completeListing()).
        long id = publishedWithToken(owner, admin, token, l -> l.put("genderPolicy", "FEMALE_ONLY"));

        expectCount(1, "q", token);
        expectCount(1, "q", token, "maxRent", "9500");
        expectCount(0, "q", token, "maxRent", "9000");
        expectCount(1, "q", token, "roomType", "DOUBLE");
        expectCount(0, "q", token, "roomType", "SINGLE");
        expectCount(1, "q", token, "suitableFor", "FEMALE");
        expectCount(0, "q", token, "suitableFor", "MALE");
        expectCount(1, "q", token, "amenities", "WIFI");
        expectCount(0, "q", token, "amenities", "AC"); // owner said "no"
        expectCount(1, "q", token, "collegeId", "1", "maxDistanceKm", "2");
        expectCount(0, "q", token, "collegeId", "1", "maxDistanceKm", "0.1");

        // Distance to VJTI is reported (straight-line), and "available only" hides full listings.
        String body = mockMvc.perform(get(SEARCH).param("q", token).param("collegeId", "1"))
                .andReturn().getResponse().getContentAsString();
        assertThat(((Number) JsonPath.read(body, "$.items[0].distanceKm")).doubleValue()).isBetween(0.1, 1.0);

        String detail = mockMvc.perform(get(OWNER_LISTINGS + "/" + id).session(owner)).andReturn().getResponse().getContentAsString();
        long roomId = ((Number) JsonPath.read(detail, "$.rooms[0].id")).longValue();
        mockMvc.perform(patch(OWNER_LISTINGS + "/" + id + "/availability").with(csrf()).session(owner)
                .contentType(MediaType.APPLICATION_JSON).content("{\"rooms\":[{\"roomId\":" + roomId + ",\"bedsAvailable\":0}]}"))
                .andExpect(status().isOk());
        expectCount(1, "q", token);
        expectCount(0, "q", token, "availableOnly", "true");
    }

    @Test
    void unknownCollegeIsRejected() throws Exception {
        mockMvc.perform(get(SEARCH).param("collegeId", "999999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("UNKNOWN_COLLEGE"));
    }

    private void expectCount(int expected, String... params) throws Exception {
        var request = get(SEARCH);
        for (int i = 0; i < params.length; i += 2) {
            request = request.param(params[i], params[i + 1]);
        }
        mockMvc.perform(request).andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(expected)));
    }
}
