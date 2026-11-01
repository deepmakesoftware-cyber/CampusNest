package com.campusnest.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    /**
     * Hashes passwords with BCrypt (slow on purpose, salted automatically).
     * Stored hashes look like "{bcrypt}$2a$10$...": the prefix records the algorithm, so we could
     * move to a stronger one later without breaking existing accounts.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
