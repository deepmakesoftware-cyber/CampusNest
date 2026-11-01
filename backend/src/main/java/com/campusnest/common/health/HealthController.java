package com.campusnest.common.health;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoint that confirms the backend is running and can reach PostgreSQL.
 * The frontend's /status page calls this to prove the full chain works:
 * browser -> Next.js -> Spring Boot -> PostgreSQL.
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ResponseEntity<HealthResponse> health() {
        boolean databaseUp = isDatabaseUp();
        HealthResponse body = new HealthResponse(
                databaseUp ? "UP" : "DEGRADED",
                databaseUp ? "UP" : "DOWN",
                Instant.now());
        // 503 tells monitoring tools (and our frontend) that something is wrong.
        return ResponseEntity.status(databaseUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    private boolean isDatabaseUp() {
        try {
            Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return one != null && one == 1;
        } catch (RuntimeException e) {
            // Deliberately not returning the error text: it could reveal connection details.
            return false;
        }
    }

    public record HealthResponse(String status, String database, Instant checkedAt) {
    }
}
