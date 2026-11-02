package com.campusnest.roommate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;

import com.campusnest.auth.Role;
import com.campusnest.support.ApiTestSupport;
import com.jayway.jsonpath.JsonPath;

/** FR-2 acceptance tests: compatible students see an explained result; private data stays private until both accept. */
class RoommateApiTest extends ApiTestSupport {

    private static final String BASE = "/api/v1/student/roommates";

    private record Student(MockHttpSession session, long id, String email) {
    }

    private Student student(String name, String gender) throws Exception {
        String email = uniqueEmail();
        register(email, Role.STUDENT);
        MockHttpSession s = login(email, PASSWORD);
        mockMvc.perform(put("/api/v1/users/me").with(csrf()).session(s).contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"" + name + "\",\"phone\":\"+91 98000 00000\"}")).andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/student/profile").with(csrf()).session(s).contentType(MediaType.APPLICATION_JSON)
                .content("{\"collegeId\":1,\"course\":\"MCA\",\"yearOfStudy\":1,\"gender\":\"" + gender + "\"}")).andExpect(status().isOk());
        return new Student(s, users.findByEmail(email).orElseThrow().getId(), email);
    }

    private static Map<String, Object> profile() {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("discoverable", true);
        p.put("budgetMin", 8000);
        p.put("budgetMax", 11000);
        p.put("preferredLocalities", List.of("Matunga", "Dadar"));
        p.put("anywhereNearCollege", false);
        p.put("sleepSchedule", "EARLY");
        p.put("smoking", "NON_SMOKER");
        p.put("cannotLiveWithSmoker", false);
        p.put("studyEnvironment", "QUIET");
        p.put("cleanliness", 4);
        p.put("guests", 2);
        p.put("foodPreference", "VEG");
        p.put("sharingSize", "ONE_ROOMMATE");
        p.put("sameGenderOnly", false);
        p.put("about", "MCA first year, early riser.");
        return p;
    }

    private ResultActions saveProfile(Student s, Map<String, Object> p) throws Exception {
        return mockMvc.perform(put(BASE + "/profile").with(csrf()).session(s.session())
                .contentType(MediaType.APPLICATION_JSON).content(new tools.jackson.databind.ObjectMapper().writeValueAsString(p)));
    }

    private ResultActions sendRequest(Student from, Student to) throws Exception {
        return mockMvc.perform(post(BASE + "/requests").with(csrf()).session(from.session())
                .contentType(MediaType.APPLICATION_JSON).content("{\"toUserId\":" + to.id() + ",\"message\":\"Hi! Looking near Matunga?\"}"));
    }

    private Map<String, Object> matchFor(Student viewer, Student other) throws Exception {
        String body = mockMvc.perform(get(BASE + "/matches").session(viewer.session()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> found = JsonPath.read(body, "$[?(@.person.userId == " + other.id() + ")]");
        return found.isEmpty() ? null : found.get(0);
    }

    @Test
    void matchesNeedYourOwnVisibleProfile() throws Exception {
        Student asha = student("Asha Kulkarni", "FEMALE");
        mockMvc.perform(get(BASE + "/profile").session(asha.session())).andExpect(status().isNoContent());
        mockMvc.perform(get(BASE + "/matches").session(asha.session()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ROOMMATE_PROFILE_REQUIRED"));

        Map<String, Object> hidden = profile();
        hidden.put("discoverable", false);
        saveProfile(asha, hidden).andExpect(status().isOk());
        mockMvc.perform(get(BASE + "/matches").session(asha.session())).andExpect(status().isConflict());
    }

    @Test
    void compatibleStudentsSeeAnExplainedMatchWithoutPrivateDetails() throws Exception {
        Student asha = student("Asha Kulkarni", "FEMALE");
        Student meera = student("Meera Joshi", "FEMALE");
        saveProfile(asha, profile()).andExpect(status().isOk());
        saveProfile(meera, profile()).andExpect(status().isOk());

        Map<String, Object> match = matchFor(asha, meera);
        assertThat(match).isNotNull();
        String json = new tools.jackson.databind.ObjectMapper().writeValueAsString(match);
        assertThat(JsonPath.<Integer>read(json, "$.compatibility.percent")).isEqualTo(100);
        assertThat(JsonPath.<String>read(json, "$.person.shortName")).isEqualTo("Meera J.");
        assertThat(JsonPath.<String>read(json, "$.relationship")).isEqualTo("NONE");
        // No full name, email or phone before both sides accept.
        assertThat(json).doesNotContain("Joshi").doesNotContain(meera.email()).doesNotContain("98000");
    }

    @Test
    void dealbreakersHideMatchesAndBlockRequests() throws Exception {
        Student asha = student("Asha Kulkarni", "FEMALE");
        Student rohan = student("Rohan Mehta", "MALE");
        Map<String, Object> sameGender = profile();
        sameGender.put("sameGenderOnly", true);
        saveProfile(asha, sameGender).andExpect(status().isOk());
        saveProfile(rohan, profile()).andExpect(status().isOk());

        assertThat(matchFor(rohan, asha)).isNull();
        sendRequest(rohan, asha).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("NOT_COMPATIBLE"));
    }

    @Test
    void contactDetailsAreSharedOnlyAfterTheRequestIsAccepted() throws Exception {
        Student asha = student("Asha Kulkarni", "FEMALE");
        Student meera = student("Meera Joshi", "FEMALE");
        saveProfile(asha, profile()).andExpect(status().isOk());
        saveProfile(meera, profile()).andExpect(status().isOk());

        String sent = sendRequest(asha, meera).andExpect(status().isCreated())
                .andExpect(jsonPath("$.direction").value("SENT"))
                .andReturn().getResponse().getContentAsString();
        long requestId = ((Number) JsonPath.read(sent, "$.id")).longValue();
        sendRequest(asha, meera).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("REQUEST_EXISTS"));
        sendRequest(meera, asha).andExpect(status().isConflict()); // one active connection per pair, either direction

        mockMvc.perform(get(BASE + "/requests").session(meera.session()))
                .andExpect(jsonPath("$[?(@.id == " + requestId + ")].direction").value("RECEIVED"));
        mockMvc.perform(get(BASE + "/connections").session(asha.session())).andExpect(jsonPath("$.length()").value(0));

        // Only the recipient can accept.
        mockMvc.perform(post(BASE + "/requests/" + requestId + "/accept").with(csrf()).session(asha.session()))
                .andExpect(status().isConflict());
        mockMvc.perform(post(BASE + "/requests/" + requestId + "/accept").with(csrf()).session(meera.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        mockMvc.perform(get(BASE + "/connections").session(asha.session()))
                .andExpect(jsonPath("$[0].displayName").value("Meera Joshi"))
                .andExpect(jsonPath("$[0].email").value(meera.email()))
                .andExpect(jsonPath("$[0].phone").value("+91 98000 00000"));
        assertThat(matchFor(asha, meera).get("relationship")).isEqualTo("CONNECTED");
    }

    @Test
    void requestsCanBeDeclinedOrWithdrawnAndOutsidersCannotTouchThem() throws Exception {
        Student asha = student("Asha Kulkarni", "FEMALE");
        Student meera = student("Meera Joshi", "FEMALE");
        Student priya = student("Priya Nair", "FEMALE");
        for (Student s : List.of(asha, meera, priya)) {
            saveProfile(s, profile()).andExpect(status().isOk());
        }
        long r1 = ((Number) JsonPath.read(sendRequest(asha, meera).andReturn().getResponse().getContentAsString(), "$.id")).longValue();
        mockMvc.perform(post(BASE + "/requests/" + r1 + "/accept").with(csrf()).session(priya.session()))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(BASE + "/requests/" + r1 + "/decline").with(csrf()).session(meera.session()))
                .andExpect(jsonPath("$.status").value("DECLINED"));
        mockMvc.perform(post(BASE + "/requests/" + r1 + "/accept").with(csrf()).session(meera.session()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("REQUEST_CLOSED"));

        long r2 = ((Number) JsonPath.read(sendRequest(asha, priya).andReturn().getResponse().getContentAsString(), "$.id")).longValue();
        mockMvc.perform(post(BASE + "/requests/" + r2 + "/withdraw").with(csrf()).session(asha.session()))
                .andExpect(jsonPath("$.status").value("WITHDRAWN"));
        // After a decline or withdrawal, a new request is possible.
        sendRequest(asha, priya).andExpect(status().isCreated());
    }

    @Test
    void blockingHidesBothWaysAndStopsRequests() throws Exception {
        Student asha = student("Asha Kulkarni", "FEMALE");
        Student meera = student("Meera Joshi", "FEMALE");
        saveProfile(asha, profile()).andExpect(status().isOk());
        saveProfile(meera, profile()).andExpect(status().isOk());
        sendRequest(meera, asha).andExpect(status().isCreated());

        mockMvc.perform(post(BASE + "/blocks/" + meera.id()).with(csrf()).session(asha.session())).andExpect(status().isNoContent());

        assertThat(matchFor(asha, meera)).isNull();
        assertThat(matchFor(meera, asha)).isNull();
        sendRequest(meera, asha).andExpect(status().isNotFound());
        mockMvc.perform(get(BASE + "/requests").session(asha.session()))
                .andExpect(jsonPath("$[?(@.person.userId == " + meera.id() + ")]").isEmpty());
    }

    @Test
    void invalidProfilesAndWrongRolesAreRejected() throws Exception {
        Student asha = student("Asha Kulkarni", "FEMALE");
        Map<String, Object> bad = profile();
        bad.put("budgetMin", 12000);
        bad.put("budgetMax", 9000);
        saveProfile(asha, bad).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("INVALID_BUDGET"));
        bad = profile();
        bad.put("cleanliness", 9);
        saveProfile(asha, bad).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.cleanliness").exists());

        saveProfile(asha, profile()).andExpect(status().isOk());
        mockMvc.perform(post(BASE + "/requests").with(csrf()).session(asha.session()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"toUserId\":" + asha.id() + "}")).andExpect(status().isBadRequest());

        MockHttpSession owner = registerAndLogin(Role.OWNER);
        mockMvc.perform(get(BASE + "/matches").session(owner)).andExpect(status().isForbidden());
    }
}
