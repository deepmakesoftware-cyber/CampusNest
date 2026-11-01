package com.campusnest.common.security;

import java.io.IOException;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.campusnest.common.error.ApiError;
import com.campusnest.common.web.JsonResponseWriter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Runs just before Spring Security checks a login, and refuses it (429) if that email is temporarily locked. */
public class LoginRateLimitFilter extends OncePerRequestFilter {

    // Same URL matching Spring Security uses internally (works regardless of servlet mapping).
    private static final RequestMatcher LOGIN_REQUEST =
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, SecurityConfig.LOGIN_URL);

    private final LoginAttemptService loginAttempts;
    private final JsonResponseWriter json;

    public LoginRateLimitFilter(LoginAttemptService loginAttempts, JsonResponseWriter json) {
        this.loginAttempts = loginAttempts;
        this.json = json;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !LOGIN_REQUEST.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (loginAttempts.isBlocked(request.getParameter("email"))) {
            json.write(response, HttpStatus.TOO_MANY_REQUESTS.value(), new ApiError("TOO_MANY_ATTEMPTS",
                    "Too many failed login attempts. Please wait 15 minutes and try again."));
            return;
        }
        chain.doFilter(request, response);
    }
}
