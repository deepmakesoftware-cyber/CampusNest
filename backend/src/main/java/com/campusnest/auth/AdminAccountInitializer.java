package com.campusnest.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first administrator account at startup from configuration (ADMIN_EMAIL / ADMIN_PASSWORD
 * in .env). There is deliberately no way to register as an admin through the API.
 * Does nothing if the settings are empty or the account already exists.
 */
@Component
@Order(10) // before DemoDataSeeder
public class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);
    private static final int MIN_ADMIN_PASSWORD_LENGTH = 12;

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String displayName;

    public AdminAccountInitializer(UserAccountRepository users, PasswordEncoder passwordEncoder,
            @Value("${campusnest.admin.email:}") String email,
            @Value("${campusnest.admin.password:}") String password,
            @Value("${campusnest.admin.display-name:CampusNest Admin}") String displayName) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.email = UserAccount.normalizeEmail(email);
        this.password = password;
        this.displayName = displayName;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.info("ADMIN_EMAIL/ADMIN_PASSWORD not set: no administrator account created.");
            return;
        }
        if (password.length() < MIN_ADMIN_PASSWORD_LENGTH) {
            log.warn("ADMIN_PASSWORD is shorter than {} characters: administrator account NOT created.",
                    MIN_ADMIN_PASSWORD_LENGTH);
            return;
        }
        if (users.existsByEmail(email)) {
            return;
        }
        users.save(new UserAccount(email, passwordEncoder.encode(password), Role.ADMIN, displayName));
        log.info("Created administrator account {}", email); // never log the password
    }
}
