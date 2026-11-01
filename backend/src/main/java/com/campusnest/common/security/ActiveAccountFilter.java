package com.campusnest.common.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.campusnest.auth.AccountStatus;
import com.campusnest.auth.AppUserPrincipal;
import com.campusnest.auth.UserAccountRepository;
import com.campusnest.common.error.ApiError;
import com.campusnest.common.web.JsonResponseWriter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * The session remembers who logged in, but an admin may suspend that account afterwards.
 * On every request from a logged-in user, this filter re-checks the account status in the
 * database and ends the session immediately if the account is no longer active.
 */
public class ActiveAccountFilter extends OncePerRequestFilter {

    private final UserAccountRepository users;
    private final JsonResponseWriter json;

    public ActiveAccountFilter(UserAccountRepository users, JsonResponseWriter json) {
        this.users = users;
        this.json = json;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUserPrincipal principal) {
            boolean active = users.findStatusById(principal.getId())
                    .map(status -> status == AccountStatus.ACTIVE)
                    .orElse(false); // account deleted
            if (!active) {
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                json.write(response, HttpStatus.UNAUTHORIZED.value(),
                        new ApiError("ACCOUNT_SUSPENDED", "This account has been suspended. Contact CampusNest support."));
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
