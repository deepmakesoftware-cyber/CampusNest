# CampusNest — Product Requirements Document

Status legend used across the docs: **Proposed** (planned) · **Implemented** (code exists) · **Tested** (automated tests pass).
Current status of each item is tracked in [STATUS.md](STATUS.md).

## 1. Problem statement

Students moving to Mumbai for higher education often cannot get institutional hostel accommodation because
of limited capacity and eligibility rules. They then search through scattered sources — property websites,
brokers, social-media groups and personal contacts. None of these helps a student evaluate, together:

- monthly budget, including additional recurring charges
- distance and travel time to college
- room type and sharing preferences
- amenities
- listing reliability
- roommate compatibility
- total accommodation plus commuting cost

## 2. Objectives

1. Rank accommodation for each student with an **explainable suitability score**.
2. Show the **cost-versus-commute trade-off** across multiple properties.
3. Make **listing trust** visible through owner verification, admin review, reviews, reports and freshness.
4. Help students find **compatible roommates** while protecting personal information.
5. Enforce **role-based access control** on the backend.

## 3. Scope

**In scope (academic release):** VJTI and other Mumbai colleges; three roles (Student, Owner, Admin);
listings, search, recommendations, comparison, enquiries, saved listings, roommate matching, reviews,
reports, moderation.

**Out of scope (v1):** payments, bookings, rental agreements, advertisements, paid plans, machine learning,
native mobile apps, cities outside Mumbai (the data model supports them later).

## 4. Users and roles

| Role | Can | Cannot |
|---|---|---|
| **Student** | Manage profile and preferences; search; view recommendations; compare; save; send enquiries; create opt-in roommate profile; send/accept roommate requests; review; report | Create listings; see other students' contact details before mutual acceptance; access admin functions |
| **Owner** | Manage own owner profile; request verification; create/edit/submit own listings; update availability; reply to enquiries on own listings | See or edit other owners' listings; see student roommate data; moderate |
| **Admin** | Review listings (approve / reject / request changes / suspend / reinstate); verify owners; handle reports; hide reviews; suspend/reinstate users | Self-register (admins are seeded from configuration) |

## 5. Faculty requirements → features → tests (traceability)

| ID | Requirement | Planned features | Acceptance tests |
|---|---|---|---|
| FR-1 | Accommodation Suitability Score | `SuitabilityScorer` (pure Java), `GET /recommendations`, score breakdown UI | Unit: excellent/poor budget, excellent commute, room-type mismatch, missing amenities, unverified owner, stale availability. Integration: ranked results come from real DB rows |
| FR-2 | Roommate Matching | Opt-in roommate profile, `CompatibilityScorer`, match list with matches/conflicts, connection requests | Unit: high, partial, budget-incompatible, lifestyle-conflict, incomplete profiles. Privacy: no contact data before mutual acceptance |
| FR-3 | Listing Verification & Trust | Listing state machine, owner verification, moderation queue, reviews, reports, freshness label | Every allowed transition succeeds; every forbidden transition returns 409; students never see unpublished listings |
| FR-4 | Cost vs Commute | Commute estimates per listing–college, fare rules with sources, compare up to 4 listings | Estimates labelled "Estimated" with source; total hidden when data incomplete; works with routing service off |
| FR-5 | Privacy & RBAC | Spring Security roles, service-level ownership checks, privacy-filtered DTOs | Role × endpoint matrix (wrong role → 403, anonymous → 401); owner A cannot modify owner B's listing |

## 6. Functional requirements by module

**A. Authentication & users** — register (student/owner), login, logout, current-user endpoint, profile edit,
account status (active/suspended), seeded admin.

**B. Student** — profile with college, accommodation preferences (budget range, max commute, ranked room
types, must-have / nice-to-have amenities, preferred localities), search & filters, recommendations,
comparison, saved listings, enquiries, roommate discovery, reviews, reports.

**C. Accommodation** — listing details, images, room options (type, sharing, rent, deposit, beds), additional
charges, amenities (available / not available / unknown), map location, gender policy, status, freshness.

**D. Recommendation** — eligibility filters, weighted score, per-component explanation, interpretation band,
data-completeness indicator. See [ALGORITHMS.md](ALGORITHMS.md).

**E. Roommate** — opt-in profile, dealbreakers, weighted compatibility, matches and conflicts, requests,
block/report.

**F. Owner** — owner profile, verification request, listing CRUD, submit for review, availability updates,
enquiry inbox, status and admin feedback.

**G. Admin** — dashboard counts, listing review queue, owner verification, report handling, review hiding,
user suspension, audit trail.

**H. Maps & commute** — Leaflet + OpenStreetMap display, pin-drop location entry, straight-line distance,
optional road routing (OSRM, cached), local-train estimates from an admin-maintained station table,
cost-vs-commute comparison.

## 7. Non-functional requirements

| Area | Requirement |
|---|---|
| Security | Deny-by-default API, BCrypt passwords, HttpOnly session cookie, CSRF protection, input validation, safe uploads, no secrets in Git |
| Privacy | Collect minimum data; gender and food preference optional; contact details revealed only through enquiries / mutual roommate acceptance; EXIF stripped from images |
| Performance | Search and recommendations respond in < 1 s with 1,000 listings on a student laptop |
| Usability | Responsive from 360 px; meaningful empty, loading and error states |
| Accessibility | WCAG 2.2 AA basics: labels, contrast, keyboard navigation |
| Reliability | Works fully offline from third-party routing services, with clearly labelled fallback estimates |
| Maintainability | Modular monolith; each module explainable by its owner; schema changes only via Flyway |
| Reproducibility | One `docker compose up -d` for the database; documented run commands |
| Honesty | Estimates are labelled; verification is never presented as a safety guarantee; sample data is badged |

## 8. Main user journeys

1. **Student finds accommodation:** register → set college and preferences → view ranked recommendations
   with score breakdown → open details → compare 2–4 options → save → send enquiry.
2. **Student finds a roommate:** opt in → fill roommate preferences → view matches with explanation →
   send request → on mutual acceptance, see contact details.
3. **Owner lists a property:** register → request owner verification → create draft → add rooms, charges,
   amenities, photos, map pin → submit → respond to admin feedback → listing published → update availability.
4. **Admin moderates:** open review queue → inspect listing → approve / request changes / reject with reason
   → handle reports → suspend or reinstate.

## 9. Assumptions

1. One role per account.
2. Admin accounts are seeded from environment configuration; no public admin signup.
3. VJTI is the first seeded college; the `colleges` table supports more.
4. Mumbai-only data in v1; city is stored as data, not hard-coded.
5. No payments or bookings; enquiries only.
6. Owner verification does not store government ID images (decision D4); the admin records how they verified.
7. Reviews are limited to students who sent an enquiry for that listing and are labelled "stay not verified" (D9).

## 10. Limitations (v1)

1. Commute times and costs are model estimates (straight line × road factor, assumed speeds), not live transit data;
   train routes ignore lines and transfers.
2. Local-train pass fares are not entered until an admin adds them from an official source; train cost shows
   "not available" until then.
3. Suitability weights and speeds are documented hypotheses, not yet validated with a student survey.
4. Reviews cannot prove the reviewer stayed there; they are labelled "stay not verified".
5. Owner verification is a manual identity check, not an inspection of the property.
6. No email verification, password reset, notifications, or automated frontend tests.
7. Single server; login limits are in memory. English only; desktop-first layout.

## 11. Future scope

Tracked in [BACKLOG.md](BACKLOG.md): Google sign-in, password change/reset, email verification, a step-by-step
listing wizard and assisted listing for less-educated owners, Hindi and Marathi, "use my current location".
Beyond that: more colleges and cities, notifications, timetable-based commute data if an open source becomes
available, and a short student survey to tune the scoring weights.
