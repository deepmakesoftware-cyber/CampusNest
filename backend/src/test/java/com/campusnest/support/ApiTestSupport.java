package com.campusnest.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.campusnest.TestcontainersConfiguration;
import com.campusnest.auth.Role;
import com.campusnest.auth.UserAccount;
import com.campusnest.auth.UserAccountRepository;

/**
 * Base class for API integration tests: the real application + a throwaway PostgreSQL container.
 * All subclasses share one Spring context and one container, so the suite stays fast.
 * Each test uses fresh random emails, so tests never depend on each other's data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class ApiTestSupport {

    protected static final String PASSWORD = "correct-horse-42";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserAccountRepository users;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected static String uniqueEmail() {
        return "test-" + UUID.randomUUID() + "@example.com";
    }

    protected static String registerJson(String email, String password, String displayName, String role) {
        return """
                {"email":"%s","password":"%s","displayName":"%s","role":"%s"}
                """.formatted(email, password, displayName, role);
    }

    protected MvcResult register(String email, Role role) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email, PASSWORD, "Test User", role.name())))
                .andExpect(status().isCreated())
                .andReturn();
    }

    protected MvcResult attemptLogin(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .param("email", email)
                        .param("password", password))
                .andReturn();
    }

    /** Logs in and returns the session, which later requests pass with .session(...). */
    protected MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = attemptLogin(email, password);
        if (result.getResponse().getStatus() != 200) {
            throw new AssertionError("Login failed with HTTP " + result.getResponse().getStatus());
        }
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    protected MockHttpSession registerAndLogin(Role role) throws Exception {
        String email = uniqueEmail();
        register(email, role);
        return login(email, PASSWORD);
    }

    /** Admins cannot self-register, so tests create them directly in the database. */
    protected MockHttpSession createAdminAndLogin() throws Exception {
        String email = uniqueEmail();
        users.save(new UserAccount(email, passwordEncoder.encode(PASSWORD), Role.ADMIN, "Test Admin"));
        return login(email, PASSWORD);
    }
}
