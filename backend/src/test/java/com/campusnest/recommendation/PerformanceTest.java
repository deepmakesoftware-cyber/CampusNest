package com.campusnest.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.campusnest.TestcontainersConfiguration;

/**
 * Non-functional requirement: search and recommendations respond in under 1 second with 1,000 listings.
 *
 * Runs in its OWN Spring context (different property) and therefore its own fresh database, so the 1,000 listings
 * never slow down or disturb the other tests. Data is inserted with plain SQL for speed. Timings are printed so the
 * real numbers can be quoted in the report; the assertion uses the 1-second target.
 */
@SpringBootTest(properties = {"campusnest.storage.dir=target/test-uploads", "campusnest.test.suite=performance"})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PerformanceTest {

    static final int LISTINGS = 1000;
    static final int RUNS = 5;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PasswordEncoder passwordEncoder;

    MockHttpSession student;

    @BeforeAll
    void seed() throws Exception {
        Instant now = Instant.now();
        jdbc.update("INSERT INTO users (email, password_hash, role, display_name) VALUES (?, ?, 'OWNER', 'Perf Owner')",
                "perf-owner@example.com", passwordEncoder.encode("x-not-used-123"));
        long ownerId = jdbc.queryForObject("SELECT id FROM users WHERE email = 'perf-owner@example.com'", Long.class);
        jdbc.update("INSERT INTO owner_profiles (user_id) VALUES (?)", ownerId);
        List<Long> amenityIds = jdbc.queryForList("SELECT id FROM amenities WHERE is_core", Long.class);

        Random random = new Random(42); // fixed seed: the same data every run
        List<Object[]> listings = new ArrayList<>();
        for (int i = 0; i < LISTINGS; i++) {
            listings.add(new Object[] {ownerId, "Perf listing " + i, "Generated listing for the performance test, near Mumbai.",
                    "Lane " + i, "Area " + (i % 40), "400019", 18.95 + random.nextDouble() * 0.30, 72.80 + random.nextDouble() * 0.20,
                    Timestamp.from(now)});
        }
        jdbc.batchUpdate("""
                INSERT INTO listings (owner_id, title, description, address_line, locality, pincode, latitude, longitude,
                                      status, is_sample_data, last_confirmed_at, published_at, submitted_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PUBLISHED_AVAILABLE', TRUE, ?, now(), now())
                """, listings);
        List<Long> ids = jdbc.queryForList("SELECT id FROM listings WHERE owner_id = ? ORDER BY id", Long.class, ownerId);

        List<Object[]> rooms = new ArrayList<>();
        List<Object[]> answers = new ArrayList<>();
        for (Long id : ids) {
            rooms.add(new Object[] {id, "DOUBLE", 2, 6000 + random.nextInt(9000), 4, 1 + random.nextInt(3)});
            rooms.add(new Object[] {id, "SINGLE", 1, 9000 + random.nextInt(9000), 2, random.nextInt(2)});
            for (Long a : amenityIds) {
                answers.add(new Object[] {id, a, random.nextBoolean()});
            }
        }
        jdbc.batchUpdate("INSERT INTO room_options (listing_id, room_type, sharing_capacity, monthly_rent, beds_total, beds_available) VALUES (?, ?, ?, ?, ?, ?)", rooms);
        jdbc.batchUpdate("INSERT INTO listing_amenities (listing_id, amenity_id, available) VALUES (?, ?, ?)", answers);

        // A student with college and preferences, created through the API like a real user.
        String email = "perf-student@example.com";
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/register").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"correct-horse-42\",\"displayName\":\"Perf Student\",\"role\":\"STUDENT\"}"))
                .andExpect(status().isCreated());
        student = (MockHttpSession) mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/v1/auth/login").with(csrf()).param("email", email).param("password", "correct-horse-42"))
                .andReturn().getRequest().getSession(false);
        mockMvc.perform(put("/api/v1/student/profile").with(csrf()).session(student).contentType(MediaType.APPLICATION_JSON)
                .content("{\"collegeId\":1}")).andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/student/preferences").with(csrf()).session(student).contentType(MediaType.APPLICATION_JSON)
                .content("{\"budgetMax\":11000,\"maxCommuteMinutes\":40,\"commuteMode\":\"ANY\",\"roomChoices\":[\"DOUBLE\"],"
                        + "\"mustHaveAmenities\":[\"WIFI\",\"MEALS\"],\"niceToHaveAmenities\":[\"AC\"]}"))
                .andExpect(status().isOk());
    }

    /** Average time of RUNS requests after one warm-up request (the first call also warms up the JVM). */
    private long averageMillis(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request).andExpect(status().isOk());
        long total = 0;
        for (int i = 0; i < RUNS; i++) {
            long start = System.nanoTime();
            mockMvc.perform(request).andExpect(status().isOk());
            total += System.nanoTime() - start;
        }
        return total / RUNS / 1_000_000;
    }

    @Test
    void searchWithAThousandListingsIsUnderOneSecond() throws Exception {
        long ms = averageMillis(get("/api/v1/listings").param("collegeId", "1").param("maxRent", "12000").param("amenities", "WIFI"));
        System.out.printf("PERFORMANCE: search over %d listings: average %d ms%n", LISTINGS, ms);
        assertThat(ms).isLessThan(1000);
    }

    @Test
    void recommendationsForAThousandListingsAreUnderOneSecond() throws Exception {
        long ms = averageMillis(get("/api/v1/student/recommendations").session(student));
        System.out.printf("PERFORMANCE: recommendations over %d listings: average %d ms%n", LISTINGS, ms);
        assertThat(ms).isLessThan(1000);
    }
}
