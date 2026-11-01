package com.campusnest.auth;

import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusnest.auth.AccountDtos.RegisterRequest;
import com.campusnest.auth.AccountDtos.UpdateAccountRequest;
import com.campusnest.auth.AccountDtos.UserResponse;
import com.campusnest.common.error.ApiException;
import com.campusnest.owner.OwnerProfile;
import com.campusnest.owner.OwnerProfileRepository;
import com.campusnest.student.StudentProfile;
import com.campusnest.student.StudentProfileRepository;

@Service
public class AccountService {

    private final UserAccountRepository users;
    private final StudentProfileRepository studentProfiles;
    private final OwnerProfileRepository ownerProfiles;
    private final PasswordEncoder passwordEncoder;

    public AccountService(UserAccountRepository users, StudentProfileRepository studentProfiles,
            OwnerProfileRepository ownerProfiles, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.studentProfiles = studentProfiles;
        this.ownerProfiles = ownerProfiles;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Public self-registration. Only STUDENT and OWNER may register themselves;
     * administrator accounts are created from server configuration (AdminAccountInitializer).
     * The account and its profile row are saved in one transaction: both or neither.
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (request.role() == Role.ADMIN) {
            throw ApiException.badRequest("ROLE_NOT_ALLOWED", "You can register as a student or a property owner.");
        }
        String email = UserAccount.normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw ApiException.conflict("EMAIL_TAKEN", "An account with this email already exists.");
        }

        UserAccount user = users.save(new UserAccount(email, passwordEncoder.encode(request.password()),
                request.role(), request.displayName()));

        if (user.getRole() == Role.STUDENT) {
            studentProfiles.save(new StudentProfile(user));
        } else {
            ownerProfiles.save(new OwnerProfile(user));
        }
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse recordSuccessfulLogin(Long userId) {
        UserAccount user = load(userId);
        user.recordLogin(Instant.now());
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getAccount(Long userId) {
        return UserResponse.from(load(userId));
    }

    @Transactional
    public UserResponse updateContactDetails(Long userId, UpdateAccountRequest request) {
        UserAccount user = load(userId);
        user.updateContactDetails(request.displayName(), request.phone());
        return UserResponse.from(user);
    }

    private UserAccount load(Long userId) {
        return users.findById(userId)
                .orElseThrow(() -> ApiException.notFound("ACCOUNT_NOT_FOUND", "Account not found."));
    }
}
