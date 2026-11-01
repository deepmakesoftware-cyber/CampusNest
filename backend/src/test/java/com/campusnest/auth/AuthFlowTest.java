package com.campusnest.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import com.campusnest.owner.OwnerProfileRepository;
import com.campusnest.owner.VerificationStatus;
import com.campusnest.student.StudentProfileRepository;
import com.campusnest.support.ApiTestSupport;

/** Registration, login, logout, CSRF and brute-force protection. */
class AuthFlowTest extends ApiTestSupport {

    @Autowired
    private StudentProfileRepository studentProfiles;

    @Autowired
    private OwnerProfileRepository ownerProfiles;

    // ---------- Registration ----------

    @Test
    void registerStudentCreatesAccountAndProfileWithHashedPassword() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email.toUpperCase(), PASSWORD, "Asha Patil", "STUDENT")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email)) // stored lower-case
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        UserAccount saved = users.findByEmail(email).orElseThrow();
        assertThat(saved.getPasswordHash()).startsWith("{bcrypt}").doesNotContain(PASSWORD);
        assertThat(studentProfiles.existsById(saved.getId())).isTrue();
        assertThat(ownerProfiles.existsById(saved.getId())).isFalse();
    }

    @Test
    void registerOwnerCreatesUnverifiedOwnerProfile() throws Exception {
        String email = uniqueEmail();
        register(email, Role.OWNER);

        UserAccount saved = users.findByEmail(email).orElseThrow();
        assertThat(ownerProfiles.findById(saved.getId()).orElseThrow().getVerificationStatus())
                .isEqualTo(VerificationStatus.UNVERIFIED);
    }

    @Test
    void registerWithExistingEmailInDifferentCaseIsRejected() throws Exception {
        String email = uniqueEmail();
        register(email, Role.STUDENT);

        mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email.toUpperCase(), PASSWORD, "Someone Else", "OWNER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("EMAIL_TAKEN"));
    }

    @Test
    void nobodyCanRegisterAsAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(uniqueEmail(), PASSWORD, "Sneaky", "ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ROLE_NOT_ALLOWED"));
    }

    @Test
    void invalidRegistrationReportsEachBadField() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("not-an-email", "short", "A", "STUDENT")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fields.email").exists())
                .andExpect(jsonPath("$.fields.password").exists())
                .andExpect(jsonPath("$.fields.displayName").exists());
    }

    @Test
    void unknownRoleValueIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(uniqueEmail(), PASSWORD, "Test User", "SUPERUSER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }

    @Test
    void stateChangingRequestWithoutCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register") // no .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(uniqueEmail(), PASSWORD, "Test User", "STUDENT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("CSRF_TOKEN_INVALID"));
    }

    // ---------- Login / me / logout ----------

    @Test
    void loginThenMeThenLogout() throws Exception {
        String email = uniqueEmail();
        register(email, Role.OWNER);

        MockHttpSession session = login(email, PASSWORD);

        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("OWNER"));
        assertThat(users.findByEmail(email).orElseThrow().getLastLoginAt()).isNotNull();

        mockMvc.perform(post("/api/v1/auth/logout").with(csrf()).session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginIsCaseInsensitiveForEmail() throws Exception {
        String email = uniqueEmail();
        register(email, Role.STUDENT);

        assertThat(attemptLogin(email.toUpperCase(), PASSWORD).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void wrongPasswordAndUnknownEmailGetTheSameAnswer() throws Exception {
        String email = uniqueEmail();
        register(email, Role.STUDENT);

        var wrongPassword = attemptLogin(email, "wrong-password-1").getResponse();
        var unknownEmail = attemptLogin(uniqueEmail(), "wrong-password-1").getResponse();

        assertThat(wrongPassword.getStatus()).isEqualTo(401);
        assertThat(unknownEmail.getStatus()).isEqualTo(401);
        assertThat(wrongPassword.getContentAsString()).isEqualTo(unknownEmail.getContentAsString());
    }

    @Test
    void meWithoutLoginIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginIsBlockedAfterFiveFailedAttempts() throws Exception {
        String email = uniqueEmail();
        register(email, Role.STUDENT);

        for (int i = 0; i < 5; i++) {
            assertThat(attemptLogin(email, "wrong-password-" + i).getResponse().getStatus()).isEqualTo(401);
        }
        // Even the correct password is refused while the email is locked.
        var blocked = attemptLogin(email, PASSWORD).getResponse();
        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getContentAsString()).contains("TOO_MANY_ATTEMPTS");
    }

    // ---------- Account details ----------

    @Test
    void userCanUpdateOwnNameAndPhone() throws Exception {
        MockHttpSession session = registerAndLogin(Role.STUDENT);

        mockMvc.perform(put("/api/v1/users/me")
                        .with(csrf()).session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Asha P.\",\"phone\":\"+91 98765 43210\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Asha P."))
                .andExpect(jsonPath("$.phone").value("+91 98765 43210"));
    }
}
