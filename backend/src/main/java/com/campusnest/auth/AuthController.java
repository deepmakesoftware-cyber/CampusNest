package com.campusnest.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.campusnest.auth.AccountDtos.RegisterRequest;
import com.campusnest.auth.AccountDtos.UpdateAccountRequest;
import com.campusnest.auth.AccountDtos.UserResponse;

import jakarta.validation.Valid;

/**
 * Account endpoints. Note what is NOT here:
 * - POST /api/v1/auth/login  is handled by Spring Security's login filter (configured in SecurityConfig).
 * - POST /api/v1/auth/logout is handled by Spring Security's logout filter.
 */
@RestController
public class AuthController {

    private final AccountService accounts;

    public AuthController(AccountService accounts) {
        this.accounts = accounts;
    }

    @PostMapping("/api/v1/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return accounts.register(request);
    }

    /** Who am I? Returns 401 (from Spring Security) when not logged in. */
    @GetMapping("/api/v1/auth/me")
    public UserResponse me(@AuthenticationPrincipal AppUserPrincipal me) {
        return accounts.getAccount(me.getId());
    }

    /**
     * Makes sure the browser has a CSRF token cookie (XSRF-TOKEN). Reading the token here
     * forces Spring Security to issue the cookie. The frontend calls this before its first
     * POST/PUT/DELETE and copies the cookie value into the X-XSRF-TOKEN header.
     */
    @GetMapping("/api/v1/auth/csrf")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void csrf(CsrfToken token) {
        token.getToken();
    }

    @PutMapping("/api/v1/users/me")
    public UserResponse updateMe(@AuthenticationPrincipal AppUserPrincipal me,
            @Valid @RequestBody UpdateAccountRequest request) {
        return accounts.updateContactDetails(me.getId(), request);
    }
}
