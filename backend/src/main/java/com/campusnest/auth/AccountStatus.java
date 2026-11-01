package com.campusnest.auth;

public enum AccountStatus {
    ACTIVE,
    /** Set by an administrator. A suspended user cannot log in and is logged out on their next request. */
    SUSPENDED
}
