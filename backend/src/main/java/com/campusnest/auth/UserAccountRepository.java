package com.campusnest.auth;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    /** Callers must pass an already-normalized (lower-case) email. */
    Optional<UserAccount> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<UserAccount> findByRole(Role role, Pageable pageable);

    /** Lightweight check used on every authenticated request (see ActiveAccountFilter). */
    @Query("select u.status from UserAccount u where u.id = :id")
    Optional<AccountStatus> findStatusById(Long id);
}
