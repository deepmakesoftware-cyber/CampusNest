package com.campusnest.auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Tells Spring Security how to find an account by email during login.
 * Spring Security then compares the submitted password with the stored BCrypt hash itself.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserAccountRepository users;

    public AppUserDetailsService(UserAccountRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return users.findByEmail(UserAccount.normalizeEmail(email))
                .map(AppUserPrincipal::from)
                // The caller only ever sees "invalid email or password"; this message is internal.
                .orElseThrow(() -> new UsernameNotFoundException("No account for this email"));
    }
}
