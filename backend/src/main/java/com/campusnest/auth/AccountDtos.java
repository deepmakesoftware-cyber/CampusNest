package com.campusnest.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request and response bodies for the account endpoints. Records = immutable data carriers. */
public final class AccountDtos {

    private AccountDtos() {
    }

    // Password limit 72: BCrypt only uses the first 72 bytes, so longer input would be silently ignored.
    public record RegisterRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(min = 2, max = 60) String displayName,
            @NotNull Role role) {
    }

    public record UpdateAccountRequest(
            @NotBlank @Size(min = 2, max = 60) String displayName,
            // Empty, or digits/spaces/hyphens with an optional leading "+", e.g. "+91 98765 43210".
            @Pattern(regexp = "^$|^\\+?[0-9][0-9 -]{6,18}$", message = "must be a valid phone number") String phone) {
    }

    /** What the frontend learns about the logged-in user. Never contains the password hash. */
    public record UserResponse(Long id, String email, String displayName, String phone, Role role) {

        public static UserResponse from(UserAccount u) {
            return new UserResponse(u.getId(), u.getEmail(), u.getDisplayName(), u.getPhone(), u.getRole());
        }
    }
}
