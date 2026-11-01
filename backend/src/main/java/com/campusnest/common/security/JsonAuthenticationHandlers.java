package com.campusnest.common.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import com.campusnest.auth.AccountService;
import com.campusnest.auth.AppUserPrincipal;
import com.campusnest.common.error.ApiError;
import com.campusnest.common.web.JsonResponseWriter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Makes Spring Security's built-in login and access checks answer in JSON (for our frontend)
 * instead of redirecting to HTML pages.
 */
@Component
public class JsonAuthenticationHandlers
        implements AuthenticationSuccessHandler, AuthenticationFailureHandler, AccessDeniedHandler {

    private final AccountService accounts;
    private final LoginAttemptService loginAttempts;
    private final JsonResponseWriter json;

    public JsonAuthenticationHandlers(AccountService accounts, LoginAttemptService loginAttempts,
            JsonResponseWriter json) {
        this.accounts = accounts;
        this.loginAttempts = loginAttempts;
        this.json = json;
    }

    /** Login succeeded: Spring Security has already stored the user in a fresh session. */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        loginAttempts.reset(principal.getUsername());
        json.write(response, HttpStatus.OK.value(), accounts.recordSuccessfulLogin(principal.getId()));
    }

    /**
     * Login failed. "Unknown email" and "wrong password" get the SAME message, so attackers
     * cannot use the login form to discover which emails are registered.
     */
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        if (exception instanceof DisabledException) {
            json.write(response, HttpStatus.UNAUTHORIZED.value(),
                    new ApiError("ACCOUNT_SUSPENDED", "This account has been suspended. Contact CampusNest support."));
            return;
        }
        loginAttempts.recordFailure(request.getParameter("email"));
        json.write(response, HttpStatus.UNAUTHORIZED.value(),
                new ApiError("INVALID_CREDENTIALS", "Incorrect email or password."));
    }

    /** Logged in, but not allowed (wrong role), or the CSRF token was missing/invalid. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            org.springframework.security.access.AccessDeniedException exception) throws IOException {
        ApiError body = (exception instanceof CsrfException)
                ? new ApiError("CSRF_TOKEN_INVALID", "Your session security token is missing or expired. Please retry.")
                : new ApiError("FORBIDDEN", "You do not have permission to do this.");
        json.write(response, HttpStatus.FORBIDDEN.value(), body);
    }
}
