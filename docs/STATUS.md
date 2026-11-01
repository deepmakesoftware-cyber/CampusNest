# CampusNest — Development Status

Last updated: 2026-11-01 (end of Phase 5)

Legend: **Proposed** (planned) · **Implemented** (code exists) · **Tested** (automated or recorded manual test passes).

## Phase 5 — Accommodation Suitability Score (FR-1)

| Feature | Status | Evidence |
|---|---|---|
| Flyway V4: `student_preferences` | Tested | Applied locally and in test containers |
| Preferences API: max budget, max commute, travel mode, ranked room types, must-have / nice-to-have amenities, validation | Tested | `RecommendationApiTest.invalidPreferencesAreRejected` |
| `SuitabilityScorer`: eligibility (full / gender / > 30% over budget), budget, commute curve, room rank, amenities (unknown = half), trust (verified, freshness, Bayesian rating, reports), bands, data completeness, plain-English explanations | Tested | `SuitabilityScorerTest` — 19 cases incl. the 7 faculty cases: excellent budget, poor budget, excellent commute, room-type mismatch, missing amenity info, unverified owner, stale/missing availability |
| Approximate commute (straight-line × 1.3 at documented assumed speeds), always labelled approximate | Tested | `ApproximateCommuteTest` (4) |
| Ranked recommendations from real stored listings, with counts of excluded listings by reason; single-listing breakdown | Tested | `RecommendationApiTest` (hand-calculated totals 90 and 65 match) |
| Frontend: preferences form, recommendations page (rank, score badge, band, expandable breakdown with the arithmetic shown), "Your suitability" panel on listing pages | Implemented | Lint, type-check, build pass; demo student's recommendations checked through the proxy (11 ranked, 1 excluded as full) |
| Demo student has preferences pre-filled (seeder is idempotent and fills them in on an existing demo database) | Tested (manual) | Startup + recommendations call |

**Test totals (end of Phase 5):** backend `mvnw test` — 125 tests, 0 failures.

Bug found and fixed in Phase 5: saving preferences for the first time inserted an empty row before the values were
set, which the database CHECK constraint rejected (caught by `RecommendationApiTest`).

## Phase 4 — Student accommodation discovery

| Feature | Status | Evidence |
|---|---|---|
| Flyway V3: `saved_listings`, `enquiries`, `enquiry_messages` | Tested | Applied locally and in test containers |
| Public search: only published listings of active owners; text, max rent, room type, "accommodation for" (gender policy), must-have amenities (owner said YES), free beds only, max distance; sort by distance / rent / freshness; pagination | Tested | `PublicDiscoveryTest` (5 tests, 11 filter assertions) |
| Straight-line distance to the chosen college (Haversine, labelled "straight-line") | Tested | `GeoDistanceTest` (5), distance assertions in `PublicDiscoveryTest` |
| Public listing page: no owner email/phone, owner verification status, freshness (fresh ≤ 14 days, aging ≤ 30, stale), sample badge | Tested | `publicDetailNeverExposesOwnerContactDetails` |
| Listings of suspended owner accounts disappear from search, detail and public photos | Tested | `listingsOfSuspendedOwnersDisappear` |
| Saved listings (idempotent save/unsave; kept but marked "no longer listed" if suspended) | Tested | `EngagementTest` (3 tests) |
| Private enquiry threads: one per student+listing, participants only (others incl. admins get 404), opt-in contact sharing per person, 15 new enquiries/day limit | Tested | `EngagementTest` (4 tests) |
| Demo data seeder (`DEMO_DATA=true`): 12 sample listings around VJTI, 4 sample owners (2 identity-checked), 1 sample student; @example.com accounts; generated "SAMPLE PHOTO" images; varied freshness; goes through the real state machine | Tested (manual) | Startup log + search results checked through the proxy |
| Frontend: search page (filters, list and map views), public listing page (trust panel, save, message owner with opt-in sharing), saved page, messages inbox and thread, role-aware navigation | Implemented | Lint, type-check, build pass; save + enquiry flow checked through the proxy with demo accounts. **Not yet clicked through in a browser by the AI** |

**Test totals (end of Phase 4):** backend `mvnw test` — 98 tests, 0 failures.

Bugs found and fixed in Phase 4: optional yes/no fields in request bodies were rejected when omitted (Jackson 3
default) — `shareContact` is now optional and `mandatory` is a clear required field; a message form defined inside
another component would have lost focus on every keystroke.

## Phase 3 — Accommodation listing management (FR-3)

| Feature | Status | Evidence |
|---|---|---|
| Flyway V2: listings, room options, charges, amenities (15 seeded, 8 core), listing amenities, images, status history | Tested | Applied to local DB and test containers |
| Listing state machine (7 states) as an explicit transition table | Tested | `ListingLifecycleTest` (23 cases incl. every allowed and key forbidden move) |
| Owner: create draft, edit everything (rooms, charges, amenity yes/no/unknown, location), delete drafts only | Tested | `ListingWorkflowTest` |
| Submission checklist (description, address, PIN, Mumbai-region location, ≥1 room, ≥1 photo, all core amenities) | Tested | `incompleteListingCannotBeSubmitted`, `locationOutsideMumbaiIsNotAccepted` |
| Locked while under review; rejected listings locked permanently | Tested | `submitThenApprove…`, `rejectedListingIsLockedForever` |
| Admin queue (oldest first, drafts never shown), approve / request changes / reject / suspend / reinstate, reasons required | Tested | `ListingWorkflowTest` (7 tests) |
| Approval publishes as Available or Full depending on free beds; reinstated → Full until owner re-confirms | Tested | `listingWithNoFreeBedsIsPublishedAsUnavailable`, `suspended…Reinstated` |
| Owner availability update / re-confirmation (freshness timestamp), auto Available ↔ Full | Tested | `availabilityUpdatesSwitch…` |
| Material edits (title, description, address, location, type, gender policy, amenities, photos) → back to review; rent/charges/beds → no re-review | Tested | `rentChangeStaysPublishedButTitleChangeNeedsNewReview` |
| Audit history of every status change with actor role and reason | Tested | history assertions in `ListingWorkflowTest` |
| Ownership isolation (other owners get 404), students can't manage listings, owners can't moderate | Tested | `ownersCannotTouchEachOthersListings`, `studentsCannotManage…` |
| Photo upload: JPEG/PNG by magic bytes, ≤ 5 MB, ≤ 6000 px, re-encoded (EXIF/GPS stripped), random file names, max 10 | Tested | `ImageProcessorTest` (6), `nonImageUploadIsRejected` |
| Photo access: public only when published; owner always; admin except drafts | Tested | `draftPhotosAreVisibleOnlyToTheirOwner`, suspension test |
| Optimistic locking (owner edit vs admin decision at the same time → 409) | Implemented | `@Version` + handler; not yet covered by a concurrent test |
| Frontend: owner listings dashboard, listing editor (map pin-drop with Leaflet + OSM, rooms, charges, amenities, photos, checklist, availability, history), admin queue and review page | Implemented | Lint, type-check, production build pass; E2E script through proxy 31/31. **Not yet clicked through in a browser by the AI** |

**Test totals (2026-11-01, end of Phase 3):** backend `mvnw test` — 81 tests, 0 failures. Listing E2E script
through the Next.js proxy (real multipart upload) — 31 checks, 0 failures.

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

- **Phase 6:** commute estimates with local-train stations and fares, cost-versus-commute comparison.
- Phases 7–10 as per [PROJECT-PLAN.md](PROJECT-PLAN.md).
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

| T13 | A material edit sends a published listing back to review, which hides it from students until re-approved | Simple and safe for v1; a later version could keep the approved version live while edits are reviewed |
| T14 | Photo files are deleted from disk before the database transaction commits; a rollback could leave a DB row without its file | Rare; move deletion to an after-commit hook if it ever happens |
| T16 | OpenStreetMap public tiles: no SLA, light use only | Tile URL is configurable (`NEXT_PUBLIC_MAP_TILE_URL`); coordinates can always be typed manually |
| T17 | Search filters run in Java over all visible listings (simple, fine for hundreds of listings) | Move filters into the SQL query if listings reach many thousands; API stays the same |
| T18 | No unread-message counts or notifications for enquiries | Later (would need e-mail or push; must stay free) |
| T19 | Suitability weights and commute speed assumptions are documented hypotheses, not validated | Short survey of ~20 VJTI students (planned for documentation phase) |
| T20 | Recommendations are computed on every request (fine for hundreds of listings) | Cache per student if listings grow large |

Resolved in Phase 2: T1 (generated default password log), T6 (Flyway "no migrations" warning).
Resolved 2026-11-01: T15 (E2E test accounts and listings removed from the local dev database; admin kept).
