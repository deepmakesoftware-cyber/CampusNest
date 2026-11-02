# CampusNest — REST API Reference

The full endpoint list (method, path, role, purpose) is in [ARCHITECTURE.md §6](ARCHITECTURE.md#6-api-design-proposed-all-under-apiv1).
This page explains the conventions every endpoint follows, with examples.

## Conventions

- Base path `/api/v1`. The browser calls the Next.js server, which forwards `/api/*` to Spring Boot (same origin,
  so the session cookie just works).
- JSON in and out (`Content-Type: application/json`), except login (form fields) and photo upload (multipart).
- **Authentication:** a session cookie (`JSESSIONID`, HttpOnly, SameSite=Lax, 2-hour idle timeout) set by `POST /auth/login`.
- **CSRF:** every POST/PUT/PATCH/DELETE must send the `X-XSRF-TOKEN` header with the value of the `XSRF-TOKEN`
  cookie. The frontend's `api.ts` does this automatically.
- **Role areas:** `/student/**`, `/owner/**`, `/admin/**`. Public: health, college list, published listings and
  their reviews, registration and login. Everything else requires login.
- **Ownership:** an owner asking for another owner's listing gets `404`, not `403`, so IDs cannot be probed.
- Money is whole rupees (integers). Times are ISO-8601 instants in UTC.

## Error format

Every error has the same shape. `error` is a stable code for programs; `message` is safe to show to users;
`fields` appears only for field-level problems.

```json
{"error": "VALIDATION_FAILED", "message": "Please correct the highlighted fields.",
 "fields": {"email": "must be a well-formed email address"}}
```

| HTTP | Typical `error` codes | Meaning |
|---|---|---|
| 400 | `VALIDATION_FAILED`, `MALFORMED_REQUEST`, `INVALID_PARAMETER`, `FILE_MISSING`, business codes such as `LISTING_INCOMPLETE` | The request is wrong; fix and retry |
| 401 | (empty body when simply not logged in), `INVALID_CREDENTIALS`, `ACCOUNT_SUSPENDED` | Not logged in / login failed |
| 403 | `FORBIDDEN`, `CSRF_TOKEN_INVALID` | Logged in but wrong role, or missing CSRF token |
| 404 | `*_NOT_FOUND` | Does not exist **or** is not yours |
| 409 | `EMAIL_TAKEN`, `CONCURRENT_UPDATE`, `CONFLICT`, invalid state changes | Conflicts with current data |
| 413 | `FILE_TOO_LARGE` | Photo over 5 MB |
| 429 | `TOO_MANY_ATTEMPTS` | 5 failed logins for this email in 15 minutes |
| 503 | — (health shows `DEGRADED`) | Database unreachable |

## Examples (curl, through the frontend proxy)

```bash
B=http://localhost:3000/api/v1
# 1. Get a CSRF cookie, then log in (form fields)
curl -s -c jar -b jar $B/auth/csrf > /dev/null
XSRF=$(grep XSRF-TOKEN jar | awk '{print $7}')
curl -s -c jar -b jar -H "X-XSRF-TOKEN: $XSRF" \
     --data-urlencode "email=student.asha@example.com" --data-urlencode "password=$DEMO_PASSWORD" $B/auth/login

# 2. Read: ranked recommendations with score breakdown
curl -s -b jar $B/student/recommendations

# 3. Write: save a listing (CSRF header required)
curl -s -b jar -c jar -H "X-XSRF-TOKEN: $XSRF" -X PUT $B/student/saved/<listingId>

# 4. Wrong role → 403
curl -s -b jar $B/admin/users
# {"error":"FORBIDDEN","message":"You do not have permission to do this."}
```

`scripts/smoke-test.sh` exercises 29 endpoints this way and is the quickest working reference.
