package com.campusnest.common.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.campusnest.auth.UserAccount;

/**
 * Brute-force protection: after MAX_FAILURES wrong passwords for the same email within WINDOW,
 * further login attempts for that email are refused until the window expires.
 *
 * Kept in memory: simple and enough for a single server. Limitations (documented):
 * counters reset when the server restarts, and someone who knows a user's email can
 * temporarily lock that user out (a deliberate trade-off against password guessing).
 */
@Component
public class LoginAttemptService {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int CLEANUP_THRESHOLD = 10_000;

    private record Attempts(int failures, Instant windowStart) {
    }

    private final Map<String, Attempts> attemptsByEmail = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptService() {
        this(Clock.systemUTC());
    }

    /** For tests: lets a test control "now". */
    LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    public boolean isBlocked(String email) {
        String key = UserAccount.normalizeEmail(email);
        if (key == null) {
            return false;
        }
        Attempts attempts = attemptsByEmail.get(key);
        if (attempts == null) {
            return false;
        }
        if (isExpired(attempts)) {
            attemptsByEmail.remove(key);
            return false;
        }
        return attempts.failures() >= MAX_FAILURES;
    }

    public void recordFailure(String email) {
        String key = UserAccount.normalizeEmail(email);
        if (key == null) {
            return;
        }
        attemptsByEmail.compute(key, (k, current) -> (current == null || isExpired(current))
                ? new Attempts(1, clock.instant())
                : new Attempts(current.failures() + 1, current.windowStart()));
        // Stop the map growing forever if someone tries thousands of random emails.
        if (attemptsByEmail.size() > CLEANUP_THRESHOLD) {
            attemptsByEmail.values().removeIf(this::isExpired);
        }
    }

    public void reset(String email) {
        String key = UserAccount.normalizeEmail(email);
        if (key != null) {
            attemptsByEmail.remove(key);
        }
    }

    private boolean isExpired(Attempts attempts) {
        return attempts.windowStart().plus(WINDOW).isBefore(clock.instant());
    }
}
