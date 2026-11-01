# CampusNest — Development Status

Last updated: 2026-11-01 (end of Phase 1)

## Completed

| Feature | Status | Evidence |
|---|---|---|
| Phase 0 plan and documentation | Implemented | `docs/` |
| Git repository, `.gitignore`, `.env.example` | Implemented | `.env` verified git-ignored |
| PostgreSQL 18 via Docker Compose | Tested (manual) | Container healthy; `select version()` returns 18.6 |
| JDK 25 LTS (Microsoft Build of OpenJDK 25.0.5) in `%USERPROFILE%\.jdks` | Installed | SHA-256 verified; system `JAVA_HOME` (JDK 27) left unchanged |
| Spring Boot 4.1.2 backend skeleton (Java 25 target) | Tested | `mvnw clean test` on JDK 25: 3 tests, 0 failures |
| Deny-by-default security config, JSON 401, CORS allow-list | Tested | `HealthControllerTest.unknownEndpointsRequireAuthentication` |
| `GET /api/v1/health` with database check | Tested | `HealthControllerTest.healthIsPublicAndReportsDatabaseUp`; manual: 503 DEGRADED within 3 s when DB stopped, recovers on restart |
| Next.js 16 frontend skeleton with Tailwind | Tested (lint/type-check) | `npm run lint`, `tsc --noEmit` clean |
| Next.js → Spring Boot proxy (`/api/*`) | Tested (manual) | `curl localhost:3000/api/v1/health` returns backend JSON |
| `/status` page | Implemented | Renders; live check runs in browser |

## Pending (next phases)

- Phase 2: users table (first Flyway migration), registration, login/logout, roles, CSRF setup, rate limiting.
- Phases 3–10 as per [PROJECT-PLAN.md](PROJECT-PLAN.md).

## Known issues / technical debt

| # | Item | Plan |
|---|---|---|
| T1 | Spring Boot logs a generated default password at startup (no real user store yet) | Removed in Phase 2 when the database-backed `UserDetailsService` is added |
| T2 | Mockito warns that self-attaching will stop working on future JDKs | Configure Mockito as a Java agent in the Surefire plugin when Mockito is first used |
| T3 | Maven prints 401 warnings for two Flyway community repositories while resolving metadata | Harmless (dependencies resolve from Maven Central); revisit if it ever blocks a build |
| T4 | `/status` is a developer page and is publicly reachable | Fine for development; hide or protect before any public deployment |
| T5 | Frontend has no automated component tests yet | Added with the first real UI in Phase 2 |
| T6 | Flyway warns "No migrations found" at startup | Expected until the first migration (`V1__…sql`) in Phase 2 |
| T7 | Eclipse Temurin download from GitHub was too slow on this network (~40 KB/s), so the Microsoft Build of OpenJDK 25 was used instead | Both are free, TCK-certified OpenJDK 25 LTS builds; teammates may use either |
