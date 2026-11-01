package com.campusnest.common.error;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The single JSON shape for every error the API returns, e.g.
 * {"error":"VALIDATION_FAILED","message":"Please correct the highlighted fields.","fields":{"email":"must be a well-formed email address"}}
 *
 * "error" is a stable code for the frontend to check; "message" is safe to show to users.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String error, String message, Map<String, String> fields) {

    public ApiError(String error, String message) {
        this(error, message, null);
    }
}
