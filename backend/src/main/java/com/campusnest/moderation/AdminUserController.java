package com.campusnest.moderation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campusnest.auth.Role;
import com.campusnest.common.web.PageResponse;
import com.campusnest.moderation.AdminUserService.AdminUserView;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUsers;

    public AdminUserController(AdminUserService adminUsers) {
        this.adminUsers = adminUsers;
    }

    /** GET /api/v1/admin/users?role=STUDENT&page=0 (both parameters optional). */
    @GetMapping
    public PageResponse<AdminUserView> list(@RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "0") int page) {
        return adminUsers.list(role, page);
    }

    @PostMapping("/{id}/suspend")
    public AdminUserView suspend(@PathVariable Long id, @Valid @RequestBody SuspendRequest request) {
        return adminUsers.suspend(id, request.reason());
    }

    @PostMapping("/{id}/reinstate")
    public AdminUserView reinstate(@PathVariable Long id) {
        return adminUsers.reinstate(id);
    }

    /** A reason is mandatory, so every suspension is explainable later. */
    public record SuspendRequest(@NotBlank @Size(max = 300) String reason) {
    }
}
