package com.campusnest.auth;

import java.io.Serial;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * The logged-in user as Spring Security sees it. It is stored in the HTTP session,
 * so it holds only what authorization needs: id, email and role.
 * The password hash is erased right after login (CredentialsContainer).
 */
public final class AppUserPrincipal implements UserDetails, CredentialsContainer {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String email;
    private final Role role;
    private final boolean active;
    private String passwordHash;

    private AppUserPrincipal(Long id, String email, Role role, boolean active, String passwordHash) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.active = active;
        this.passwordHash = passwordHash;
    }

    public static AppUserPrincipal from(UserAccount user) {
        return new AppUserPrincipal(user.getId(), user.getEmail(), user.getRole(), user.isActive(), user.getPasswordHash());
    }

    public Long getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    /** A suspended account is "disabled": Spring Security refuses the login with DisabledException. */
    @Override
    public boolean isEnabled() {
        return active;
    }

    @Override
    public void eraseCredentials() {
        this.passwordHash = null;
    }
}
