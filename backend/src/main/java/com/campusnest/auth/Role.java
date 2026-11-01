package com.campusnest.auth;

/** The three kinds of account. Spring Security sees these as authorities "ROLE_STUDENT" etc. */
public enum Role {
    STUDENT,
    OWNER,
    ADMIN
}
