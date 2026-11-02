package com.campusnest.common.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.campusnest.auth.UserAccountRepository;
import com.campusnest.common.web.JsonResponseWriter;

/**
 * Central security rules for the whole API.
 *
 * 1. Deny by default: every endpoint needs a logged-in user unless listed as public below.
 * 2. Role areas: /api/v1/student/** = STUDENT, /api/v1/owner/** = OWNER, /api/v1/admin/** = ADMIN.
 *    These checks run on the server, so hiding a button in the frontend is never the only protection.
 * 3. Login uses Spring Security's own tested login filter (session fixation protection, CSRF token
 *    rotation and session storage come built in). It accepts a form-encoded body: email=...&password=...
 * 4. CSRF protection is on for every state-changing request (POST/PUT/PATCH/DELETE), including login.
 */
@Configuration
public class SecurityConfig {

    static final String LOGIN_URL = "/api/v1/auth/login";
    static final String LOGOUT_URL = "/api/v1/auth/logout";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JsonAuthenticationHandlers handlers,
            LoginAttemptService loginAttempts, UserAccountRepository users, JsonResponseWriter json) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/health", "/api/v1/colleges", "/api/v1/amenities",
                                "/api/v1/fare-rules", "/api/v1/auth/csrf").permitAll()
                        // Photo access is decided per image in ListingImageController (public only if published).
                        .requestMatchers(HttpMethod.GET, "/api/v1/listing-images/**").permitAll()
                        // Published listings are public (the service only ever returns published ones).
                        .requestMatchers(HttpMethod.GET, "/api/v1/listings", "/api/v1/listings/*",
                                "/api/v1/listings/*/reviews").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                        .requestMatchers("/api/v1/student/**").hasRole("STUDENT")
                        .requestMatchers("/api/v1/owner/**").hasRole("OWNER")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                // Cookie-based CSRF tokens suitable for a JavaScript frontend:
                // the XSRF-TOKEN cookie is readable by our JS, which echoes it in the X-XSRF-TOKEN header.
                .csrf(csrf -> csrf.spa())
                .formLogin(form -> form
                        .loginPage("/login") // the Next.js login page; also disables Spring's default HTML login page
                        .loginProcessingUrl(LOGIN_URL)
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(handlers)
                        .failureHandler(handlers))
                .logout(logout -> logout
                        .logoutUrl(LOGOUT_URL)
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
                        .deleteCookies("JSESSIONID"))
                .exceptionHandling(ex -> ex
                        // Not logged in -> 401 (not a redirect, this is a JSON API)
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        // Logged in but wrong role, or bad CSRF token -> 403 JSON
                        .accessDeniedHandler(handlers))
                .httpBasic(AbstractHttpConfigurer::disable)
                .addFilterBefore(new LoginRateLimitFilter(loginAttempts, json), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new ActiveAccountFilter(users, json), AuthorizationFilter.class)
                .cors(Customizer.withDefaults());
        return http.build();
    }

    /**
     * In normal use the browser talks to Next.js, which forwards /api/* to us (same origin),
     * so CORS is not involved. This allow-list is a safety net for direct calls during development.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${campusnest.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
