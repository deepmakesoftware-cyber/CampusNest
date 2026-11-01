package com.campusnest.moderation;

import java.time.Instant;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusnest.auth.AccountStatus;
import com.campusnest.auth.Role;
import com.campusnest.auth.UserAccount;
import com.campusnest.auth.UserAccountRepository;
import com.campusnest.common.error.ApiException;
import com.campusnest.common.web.PageResponse;

/** Administrator actions on user accounts. Only reachable through /api/v1/admin/** (ADMIN role). */
@Service
public class AdminUserService {

    static final int PAGE_SIZE = 20;

    private final UserAccountRepository users;

    public AdminUserService(UserAccountRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserView> list(Role role, int page) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = (role == null) ? users.findAll(pageable) : users.findByRole(role, pageable);
        return PageResponse.of(result, AdminUserView::from);
    }

    @Transactional
    public AdminUserView suspend(Long userId, String reason) {
        UserAccount user = load(userId);
        // Admins cannot suspend administrators (including themselves), so the platform can't be locked out.
        if (user.getRole() == Role.ADMIN) {
            throw ApiException.conflict("CANNOT_SUSPEND_ADMIN", "Administrator accounts cannot be suspended here.");
        }
        user.suspend(reason);
        return AdminUserView.from(user);
    }

    @Transactional
    public AdminUserView reinstate(Long userId) {
        UserAccount user = load(userId);
        user.reinstate();
        return AdminUserView.from(user);
    }

    private UserAccount load(Long userId) {
        return users.findById(userId)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found."));
    }

    /** What an admin sees about an account. Phone numbers are left out: admins don't need them here. */
    public record AdminUserView(Long id, String email, String displayName, Role role, AccountStatus status,
            String statusReason, Instant createdAt, Instant lastLoginAt) {

        static AdminUserView from(UserAccount u) {
            return new AdminUserView(u.getId(), u.getEmail(), u.getDisplayName(), u.getRole(), u.getStatus(),
                    u.getStatusReason(), u.getCreatedAt(), u.getLastLoginAt());
        }
    }
}
