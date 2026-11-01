package com.campusnest.auth;

import java.time.Instant;
import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A login account (table "users"). Role-specific details live in StudentProfile / OwnerProfile.
 * Named UserAccount to avoid confusion with Spring Security's own "User" class.
 */
@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 254)
    private String email;

    // Only ever a BCrypt hash, never the password itself.
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Column(name = "status_reason", length = 300)
    private String statusReason;

    @Column(name = "display_name", nullable = false, length = 60)
    private String displayName;

    @Column(length = 20)
    private String phone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /** Required by JPA. */
    protected UserAccount() {
    }

    public UserAccount(String email, String passwordHash, Role role, String displayName) {
        this.email = normalizeEmail(email);
        this.passwordHash = passwordHash;
        this.role = role;
        this.displayName = displayName.trim();
        this.status = AccountStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    /** Emails are compared case-insensitively, so we always store them trimmed and lower-case. */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public void updateContactDetails(String displayName, String phone) {
        this.displayName = displayName.trim();
        this.phone = (phone == null || phone.isBlank()) ? null : phone.trim();
    }

    public void recordLogin(Instant when) {
        this.lastLoginAt = when;
    }

    public void suspend(String reason) {
        this.status = AccountStatus.SUSPENDED;
        this.statusReason = reason.trim();
    }

    public void reinstate() {
        this.status = AccountStatus.ACTIVE;
        this.statusReason = null;
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPhone() {
        return phone;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }
}
