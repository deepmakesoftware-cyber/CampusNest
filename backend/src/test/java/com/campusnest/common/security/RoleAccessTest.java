package com.campusnest.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import com.campusnest.auth.Role;
import com.campusnest.auth.UserAccount;
import com.campusnest.support.ApiTestSupport;

/**
 * FR-5: role-based access is enforced by the BACKEND. Each test calls the API directly,
 * exactly as an attacker bypassing the frontend would.
 */
class RoleAccessTest extends ApiTestSupport {

    private static final String STUDENT_AREA = "/api/v1/student/profile";
    private static final String OWNER_AREA = "/api/v1/owner/profile";
    private static final String ADMIN_AREA = "/api/v1/admin/users";

    // ---------- Access matrix ----------

    @Test
    void anonymousUserIsUnauthorizedEverywhereProtected() throws Exception {
        mockMvc.perform(get(STUDENT_AREA)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(OWNER_AREA)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(ADMIN_AREA)).andExpect(status().isUnauthorized());
    }

    @Test
    void studentCanOnlyUseStudentArea() throws Exception {
        MockHttpSession student = registerAndLogin(Role.STUDENT);
        mockMvc.perform(get(STUDENT_AREA).session(student)).andExpect(status().isOk());
        mockMvc.perform(get(OWNER_AREA).session(student))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        mockMvc.perform(get(ADMIN_AREA).session(student)).andExpect(status().isForbidden());
    }

    @Test
    void ownerCanOnlyUseOwnerArea() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);
        mockMvc.perform(get(OWNER_AREA).session(owner)).andExpect(status().isOk());
        mockMvc.perform(get(STUDENT_AREA).session(owner)).andExpect(status().isForbidden());
        mockMvc.perform(get(ADMIN_AREA).session(owner)).andExpect(status().isForbidden());
    }

    @Test
    void adminCanUseAdminAreaButNotOtherRolesAreas() throws Exception {
        MockHttpSession admin = createAdminAndLogin();
        mockMvc.perform(get(ADMIN_AREA).session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
        mockMvc.perform(get(STUDENT_AREA).session(admin)).andExpect(status().isForbidden());
        mockMvc.perform(get(OWNER_AREA).session(admin)).andExpect(status().isForbidden());
    }

    @Test
    void studentCannotSuspendUsersByCallingAdminEndpointDirectly() throws Exception {
        MockHttpSession student = registerAndLogin(Role.STUDENT);
        String victimEmail = uniqueEmail();
        register(victimEmail, Role.OWNER);
        Long victimId = users.findByEmail(victimEmail).orElseThrow().getId();

        mockMvc.perform(post(ADMIN_AREA + "/" + victimId + "/suspend").with(csrf()).session(student)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"test\"}"))
                .andExpect(status().isForbidden());
    }

    // ---------- Profiles: own data only, protected fields ----------

    @Test
    void studentCanSetCollegeAndUnknownCollegeIsRejected() throws Exception {
        MockHttpSession student = registerAndLogin(Role.STUDENT);

        mockMvc.perform(put(STUDENT_AREA).with(csrf()).session(student)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"collegeId\":1,\"course\":\"MCA\",\"yearOfStudy\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collegeName").value("Veermata Jijabai Technological Institute"))
                .andExpect(jsonPath("$.course").value("MCA"));

        mockMvc.perform(put(STUDENT_AREA).with(csrf()).session(student)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"collegeId\":999999}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("UNKNOWN_COLLEGE"));
    }

    @Test
    void ownerCannotMarkThemselvesVerified() throws Exception {
        MockHttpSession owner = registerAndLogin(Role.OWNER);

        mockMvc.perform(put(OWNER_AREA).with(csrf()).session(owner)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ownerType\":\"PG_OPERATOR\",\"businessName\":\"Sunrise PG\",\"verificationStatus\":\"VERIFIED\"}"));

        mockMvc.perform(get(OWNER_AREA).session(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("UNVERIFIED"));
    }

    // ---------- Suspension ----------

    @Test
    void suspendedUserIsLoggedOutImmediatelyAndCannotLogInUntilReinstated() throws Exception {
        MockHttpSession admin = createAdminAndLogin();
        String email = uniqueEmail();
        register(email, Role.STUDENT);
        MockHttpSession student = login(email, PASSWORD);
        Long studentId = users.findByEmail(email).orElseThrow().getId();

        mockMvc.perform(post(ADMIN_AREA + "/" + studentId + "/suspend").with(csrf()).session(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Fake listing reports\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        // The student's existing session stops working on the very next request.
        mockMvc.perform(get(STUDENT_AREA).session(student))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ACCOUNT_SUSPENDED"));
        // And a fresh login is refused with a clear reason.
        mockMvc.perform(post("/api/v1/auth/login").with(csrf()).param("email", email).param("password", PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ACCOUNT_SUSPENDED"));

        mockMvc.perform(post(ADMIN_AREA + "/" + studentId + "/reinstate").with(csrf()).session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        login(email, PASSWORD); // works again
    }

    @Test
    void suspensionRequiresReason() throws Exception {
        MockHttpSession admin = createAdminAndLogin();
        String email = uniqueEmail();
        register(email, Role.OWNER);
        Long id = users.findByEmail(email).orElseThrow().getId();

        mockMvc.perform(post(ADMIN_AREA + "/" + id + "/suspend").with(csrf()).session(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.reason").exists());
    }

    @Test
    void administratorsCannotBeSuspended() throws Exception {
        MockHttpSession admin = createAdminAndLogin();
        UserAccount otherAdmin = users.save(
                new UserAccount(uniqueEmail(), passwordEncoder.encode(PASSWORD), Role.ADMIN, "Other Admin"));

        mockMvc.perform(post(ADMIN_AREA + "/" + otherAdmin.getId() + "/suspend").with(csrf()).session(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"test\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CANNOT_SUSPEND_ADMIN"));
    }

    @Test
    void adminUserListNeverContainsPasswordHashes() throws Exception {
        MockHttpSession admin = createAdminAndLogin();
        String body = mockMvc.perform(get(ADMIN_AREA).session(admin)).andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("bcrypt").doesNotContain("password");
    }
}
