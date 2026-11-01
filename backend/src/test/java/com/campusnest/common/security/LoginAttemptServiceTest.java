package com.campusnest.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

/** Plain unit test (no Spring, no database): we control time with a fake clock. */
class LoginAttemptServiceTest {

    /** A clock whose "now" the test can move forward. */
    private static final class FakeClock extends Clock {
        private Instant now = Instant.parse("2026-11-01T10:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    private final FakeClock clock = new FakeClock();
    private final LoginAttemptService service = new LoginAttemptService(clock);

    @Test
    void fourFailuresAreAllowedFifthBlocks() {
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES - 1; i++) {
            service.recordFailure("a@example.com");
        }
        assertThat(service.isBlocked("a@example.com")).isFalse();

        service.recordFailure("a@example.com");
        assertThat(service.isBlocked("a@example.com")).isTrue();
    }

    @Test
    void blockExpiresAfterWindow() {
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES; i++) {
            service.recordFailure("a@example.com");
        }
        clock.advance(LoginAttemptService.WINDOW.plusSeconds(1));
        assertThat(service.isBlocked("a@example.com")).isFalse();
    }

    @Test
    void emailIsCaseInsensitiveAndOtherEmailsUnaffected() {
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES; i++) {
            service.recordFailure("A@Example.com");
        }
        assertThat(service.isBlocked("a@example.com")).isTrue();
        assertThat(service.isBlocked("b@example.com")).isFalse();
    }

    @Test
    void successfulLoginResetsCounter() {
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES - 1; i++) {
            service.recordFailure("a@example.com");
        }
        service.reset("a@example.com");
        service.recordFailure("a@example.com");
        assertThat(service.isBlocked("a@example.com")).isFalse();
    }

    @Test
    void missingEmailIsNeverBlocked() {
        service.recordFailure(null);
        assertThat(service.isBlocked(null)).isFalse();
    }
}
