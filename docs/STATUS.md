# CampusNest — Development Status

Last updated: 2026-11-01 (end of Phase 2)

Legend: **Proposed** (planned) · **Implemented** (code exists) · **Tested** (automated or recorded manual test passes).

## Phase 2 — Authentication and authorization

| Feature | Status | Evidence |
|---|---|---|
| Flyway migration V1: `users`, `colleges` (VJTI seeded), `student_profiles`, `owner_profiles` | Tested | Applied to local DB and to every test container |
| Registration (STUDENT / OWNER only), BCrypt hashing, profile row created | Tested | `AuthFlowTest` (6 registration tests) |
| Login / logout with HttpOnly session cookie (Spring Security form login, JSON responses) | Tested | `AuthFlowTest.loginThenMeThenLogout`; E2E: cookie is HttpOnly |
| Same error for unknown email and wrong password | Tested | `AuthFlowTest.wrongPasswordAndUnknownEmailGetTheSameAnswer` |
| CSRF protection on every state-changing request (cookie + header) | Tested | `AuthFlowTest.stateChangingRequestWithoutCsrfTokenIsForbidden`; E2E header path |
| Brute-force protection (5 failures / 15 min per email → 429) | Tested | `LoginAttemptServiceTest` (5), `AuthFlowTest.loginIsBlockedAfterFiveFailedAttempts` |
| Role areas `/student/**`, `/owner/**`, `/admin/**` enforced on the backend | Tested | `RoleAccessTest` access matrix (anonymous 401, wrong role 403) |
| Owner cannot change own verification status | Tested | `RoleAccessTest.ownerCannotMarkThemselvesVerified` |
| Admin seeded from `.env`, no public admin signup | Tested | `AuthFlowTest.nobodyCanRegisterAsAdmin`; startup log "Created administrator account" |
| Admin user list, suspend (reason required), reinstate; admins can't be suspended | Tested | `RoleAccessTest` (4 tests) |
| Suspended user logged out on next request and cannot log in | Tested | `RoleAccessTest.suspendedUserIsLoggedOutImmediately…`; E2E |
| Account details (name, phone), student profile (college, course, year, optional gender), owner profile | Tested (API) | `AuthFlowTest.userCanUpdateOwnNameAndPhone`, `RoleAccessTest.studentCanSetCollege…` |
| Frontend: login, register, account, role dashboards, admin users page, role-aware header | Implemented | Lint, type-check and production build pass; pages return 200. **Not yet clicked through in a browser by the AI** (browser automation unavailable) — manual check needed |

**Test totals (2026-11-01):** backend `mvnw test` — 32 tests, 0 failures. Browser-like E2E script through the
Next.js proxy — 27 checks, 0 failures.

## Phase 1 — Foundation

| Feature | Status | Evidence |
|---|---|---|
| Phase 0 plan and documentation | Implemented | `docs/` |
| Git repository, `.gitignore`, `.gitattributes`, `.env.example` | Implemented | `.env` verified git-ignored |
| PostgreSQL 18 via Docker Compose | Tested (manual) | Container healthy; `select version()` returns 18.6 |
| JDK 25 LTS (Microsoft Build of OpenJDK 25.0.5) in `%USERPROFILE%\.jdks` | Installed | SHA-256 verified; system `JAVA_HOME` (JDK 27) left unchanged |
| Spring Boot 4.1.2 backend (Java 25 target), deny-by-default security | Tested | `HealthControllerTest` |
| `GET /api/v1/health` with database check | Tested | 503 DEGRADED within 3 s when DB stopped, recovers on restart |
| Next.js 16 frontend with Tailwind, `/api/*` proxy to Spring Boot | Tested | lint, type-check, build; proxy verified with curl |

## Pending (next phases)

- **Phase 3:** listings (rooms, charges, amenities, images, location), submission, admin review, state machine.
- Phases 4–10 as per [PROJECT-PLAN.md](PROJECT-PLAN.md).
- Features agreed for later (e.g. Google sign-in): [BACKLOG.md](BACKLOG.md).

## Known issues / technical debt

| # | Item | Plan |
|---|---|---|
| T2 | Mockito warns that self-attaching will stop working on future JDKs | Configure Mockito as a Java agent in Surefire when Mockito is first used directly |
| T3 | Maven prints 401 warnings for two Flyway community repositories while resolving metadata | Harmless (dependencies resolve from Maven Central) |
| T4 | `/status` is a developer page and is publicly reachable | Hide or protect before any public deployment |
| T5 | No automated frontend tests yet | Playwright E2E for the demo journey (Phase 9) |
| T7 | Microsoft Build of OpenJDK 25 used instead of Temurin (GitHub downloads too slow on this network) | Both are TCK-certified OpenJDK 25 LTS builds |
| T8 | Login attempt counters are in memory: reset on restart, per-server only | Acceptable for a single server; move to the database if CampusNest ever runs multiple servers |
| T9 | Per-email lockout lets someone who knows an email lock that user out for 15 minutes | Documented trade-off vs. password guessing; could add per-IP limits later |
| T10 | Registration says "email already exists" (reveals that an account exists) | Common UX trade-off; revisit with email verification in a later release |
| T11 | No password change / reset, no email verification | Tracked as backlog items B2–B4 |
| T12 | Suspensions are recorded only as the latest status + reason on the user row (no full history) | Add an audit log table with the moderation work in Phase 8 |

Resolved in Phase 2: T1 (generated default password log), T6 (Flyway "no migrations" warning).
