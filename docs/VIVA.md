# CampusNest — Viva Preparation

Short, honest answers. Each points to where the evidence is, so any team member can open the code or test.

## Project and scope

**Q. What problem does CampusNest solve?**
Students who don't get a hostel seat search WhatsApp groups, brokers and portals with no way to compare total cost,
commute, trust or roommates. CampusNest ranks listings with an explainable score, compares cost against commute,
shows trust signals and matches roommates while hiding private details.

**Q. What is *not* in scope?**
Payments, bookings, a mobile app, real-time chat, ID document storage, live transit data. See PRD §3 and
[BACKLOG.md](BACKLOG.md).

**Q. Is the data real?**
Colleges, station locations (OpenStreetMap) and auto/bus fares (MMRTA, BEST) are from cited sources. All listings,
owners and students in the demo are **sample data**, labelled on screen. No real student data is stored.

## Architecture and technology

**Q. Describe the architecture.**
A three-tier **modular monolith**: Next.js frontend → Spring Boot REST API → PostgreSQL. The backend is one
deployable split into feature packages (auth, listing, recommendation, commute, roommate, moderation …).
See [ARCHITECTURE.md](ARCHITECTURE.md) and the component diagram in [UML.md](UML.md).

**Q. Why a monolith and not microservices?**
One team, one database, one deployment: microservices would add network calls, distributed transactions and
deployment work with no benefit at this size. Package boundaries keep the code organised so a module could be
extracted later.

**Q. Why Spring Boot, Next.js and PostgreSQL?**
Spring Boot: mature security, validation and data access, widely taught. Next.js + TypeScript: typed components,
routing and a proxy that keeps the API on the same origin. PostgreSQL: relational data with constraints, check
constraints and transactions; free and runs in Docker. All are free and open source.

**Q. Why session cookies instead of JWT?**
Sessions can be revoked immediately (suspending a user logs them out), the cookie is HttpOnly so JavaScript cannot
steal it, and there is no token refresh logic. JWT suits many independent services, which we don't have.

## Database

**Q. Explain the main tables.**
`users` (one role each) with `student_profiles` / `owner_profiles`; `listings` → `room_options`, `listing_charges`,
`listing_amenities` (with `amenities`), `listing_images`, `listing_status_history`; `enquiries` + `enquiry_messages`, `saved_listings`, `reviews`,
`reports`; `student_preferences`; `roommate_profiles`, `roommate_requests`, `roommate_blocks`;
`colleges`, `stations`, `fare_rules`; `verification_records`. ER diagram: [UML.md](UML.md), details:
[DATABASE.md](DATABASE.md).

**Q. How do you manage schema changes?**
Flyway migrations V1–V7 run automatically at start-up; Hibernate only *validates* that entities match. Every
environment gets the same schema from version-controlled SQL.

**Q. What normal form?** Mostly 3NF: repeated groups (rooms, charges, amenities, images) are separate tables. We
store free beds per room option rather than computing it, because owners update it directly.

**Q. How do you prevent two admins approving the same listing at once?**
Optimistic locking: a `version` column; the second save fails with `409 CONCURRENT_UPDATE`.

## REST API

**Q. What makes your API RESTful?** Resource URLs (`/listings/{id}`), HTTP verbs for actions, status codes with
meaning (201, 400, 401, 403, 404, 409), JSON bodies, stateless request handling apart from the login session, and
a single error format. See [API.md](API.md).

**Q. Why does another owner's listing return 404, not 403?** So that IDs cannot be probed to learn which listings
exist.

## Security and privacy (FR-5)

**Q. How are passwords stored?** BCrypt (salted, slow hash) via Spring's `DelegatingPasswordEncoder`; never logged
or returned.

**Q. How is role-based access enforced?** Three layers: URL areas in `SecurityConfig` (`/student/**` etc.),
ownership checks in services (`findByIdAndOwnerId`), and the UI hides links. Only the first two are security;
`RoleAccessTest` proves the API refuses wrong roles even if a user types the URL.

**Q. What is CSRF and how do you prevent it?** A malicious site making the browser send a request with the user's
cookie. Spring issues a token in an `XSRF-TOKEN` cookie which our JavaScript must copy into a header; another site
cannot read it. SameSite=Lax adds a second layer.

**Q. Other protections?** Login rate limit (5 failures / 15 minutes per email), same message for wrong email or
password, suspended users blocked on every request, photo checks (real image type, 5 MB, re-encoded to remove GPS
EXIF data), validation on every input, React escaping against XSS, parameterised queries against SQL injection.

**Q. How is student privacy protected in roommate matching?** Opt-in only; you see others only while visible
yourself; others see first name + last initial and habits, never email, phone, gender or exact budget; contact
details only after both accept; blocking hides both ways. Tests: `RoommateApiTest`.

## Algorithms

**Q. How is the suitability score calculated? (FR-1)**
Weighted sum of five 0–100 component scores: budget 30, commute 25, room type 15, amenities 15, trust 15. Hard
filters first (no free beds, gender policy, more than 30% over budget). Each component returns a reason, so the
student sees exactly why. Bands: 85+ excellent, 70+ good, 50+ fair. Code: `SuitabilityScorer` (pure function),
tests: `SuitabilityScorerTest`. Details: [ALGORITHMS.md](ALGORITHMS.md).

**Q. Why these weights?** They are documented hypotheses based on what students mention most (cost, then travel).
We say so honestly; validating them with a student survey is listed as future work (T19).

**Q. Why not machine learning?** No historical data to train on, and faculty asked for an *explainable* score.
A rule-based weighted model is transparent and testable.

**Q. How does the trust score work?** A 0–100 sub-score: owner identity checked 35, availability confirmed within
14 days 30 (within 30 days 15), review rating up to 25 using a Bayesian average (as if every listing already had 3
reviews of 3.5 stars, so one 5-star review can't dominate), and 10 for having no upheld reports in the last 90 days.
Only published (admin-reviewed) listings are ever ranked.

**Q. How does roommate compatibility work? (FR-2)**
Nine weighted factors (budget overlap, location, sleep schedule, cleanliness, guests, study environment, smoking,
food, sharing size). Dealbreakers (gender preference, smoking incompatibility, no budget overlap) exclude a match.
The result shows "in common" and "differences to discuss". Code: `CompatibilityScorer`.

**Q. How is commute estimated? (FR-4)**
Straight-line distance × 1.3 road factor; per mode speeds (walk 4.5 km/h, auto 18 km/h + 5 min, bus 13 km/h + 10
min). Train: walk/auto to nearest station, rail distance × 1.2 at 30 km/h + 8 min wait. Costs come only from
sourced fare rules; if no rule exists (train passes), the cost is shown as **not available**, never guessed.

**Q. Why not Google Maps?** It needs a billing account. OpenStreetMap + our own estimates are free; the estimates are
clearly labelled.

**Q. What is the listing lifecycle?** DRAFT → PENDING_REVIEW → PUBLISHED / CHANGES_REQUESTED / REJECTED;
PUBLISHED ↔ SUSPENDED; a material edit sends a published listing back to review. Transitions are a single table in
`ListingLifecycle`; any other transition is refused. Diagram: [UML.md](UML.md).

## Testing

**Q. How did you test?** 165 automated backend tests (unit tests for pure algorithms, integration tests with
MockMvc against a real PostgreSQL in Testcontainers), a full demo-journey test, a 1,000-listing performance test
(search ~115 ms, recommendations ~210 ms), a smoke test script against the running app, and manual browser checks.
Catalogue: [TEST-CASES.md](TEST-CASES.md).

**Q. Why Testcontainers instead of H2?** Tests run on the same PostgreSQL version as production, so SQL, constraints
and migrations behave identically.

**Q. What is not tested automatically?** Frontend UI (no browser E2E tests yet, T5) — covered by manual checks and
the smoke test.

## UML

**Q. Which diagrams did you draw?** Use case, component, class, ER, state machines (listing, owner verification,
roommate request, report), sequence (login, recommendations, listing review, roommate request) and activity
diagrams — all in [UML.md](UML.md), written in Mermaid so they stay in sync with code.

## Limitations and future work

Train fares not entered (needs an official source), estimates not validated against real trips, no email
verification / password reset, no notifications, no frontend E2E tests, single-server rate limiting, desktop-first
UI, English only. Future: items in [BACKLOG.md](BACKLOG.md) — Google sign-in, simpler step-by-step listing for
owners, Hindi/Marathi, "use my location".

## Team contribution

Be ready to say which module each member owns end to end (see [PROJECT-PLAN.md §3](PROJECT-PLAN.md)) and to walk
through one of its tests.
