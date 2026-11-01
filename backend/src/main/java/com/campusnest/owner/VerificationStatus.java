package com.campusnest.owner;

/**
 * Owner identity verification (separate from listing review).
 * UNVERIFIED -> PENDING -> VERIFIED | REJECTED. Only administrators change it (Phase 8).
 */
public enum VerificationStatus {
    UNVERIFIED,
    PENDING,
    VERIFIED,
    REJECTED
}
