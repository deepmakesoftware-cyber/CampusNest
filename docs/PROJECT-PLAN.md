# CampusNest — Project Plan

## 1. Decisions (approved 2026-11-01)

| # | Decision | Outcome |
|---|---|---|
| D1 | Authentication mechanism | Server-side session with HttpOnly cookie (not JWT) |
| D2 | Java version | JDK 25 LTS (installed JDK 27 is not LTS). Microsoft Build of OpenJDK 25 used on the lead's machine |
| D3 | PostgreSQL runtime | Docker container (`docker-compose.yml`) |
| D4 | Owner verification evidence | No ID document uploads; admin verifies offline and records method + notes |
| D5 | Gender field | Optional; used only for gender-specific listings and roommate preference |
| D6 | Local-train commute | Included via admin-maintained station table |
| D7 | Faculty review date / weekly time | **Open — needed to size the MVP** |
| D8 | Styling | Tailwind CSS with own design tokens |
| D9 | Review eligibility | Students who sent an enquiry for the listing; labelled "stay not verified" |
| D10 | Repository visibility | Private GitHub repo until final submission (remote not yet created) |

## 2. Roadmap

| Phase | Deliverable | Needed for faculty demo |
|---|---|---|
| 0 | Discovery and plan | — ✔ |
| 1 | Repo, frontend + backend + PostgreSQL running, health check end to end | ✔ |
| 2 | Registration, login, logout, 3 roles, protected endpoints, role navigation | demo 1, 9 |
| 3 | Owner listing CRUD, images, admin review, state machine | demo 7, 8 |
| 4 | Search, details, saved listings, enquiries, map | demo 2 |
| 5 | Suitability engine and score breakdown | demo 3, 4 |
| 6 | Commute estimates, cost-vs-commute comparison | demo 5 |
| 7 | Roommate matching | demo 6 |
| 8 | Reviews, reports, trust indicators, freshness | partial |
| 9 | Integration and full-journey tests | yes |
| 10 | Test suite, documentation, UML, viva prep, deployment | — |

If the review date is close, build thin vertical slices of phases 2–7 first and polish afterwards.

## 3. Team split (feature ownership, end to end)

| Member | Owns | Faculty requirements |
|---|---|---|
| A — Platform & Trust | Auth, RBAC, admin dashboard, listing state machine, owner verification, reviews, reports | FR-3, FR-5 |
| B — Listings & Maps | Owner listing CRUD, images, search & filters, map, commute estimates, comparison | FR-4 |
| C — Matching | Student profile & preferences, suitability engine, roommate matching, enquiries, saved listings | FR-1, FR-2 |

Shared: design system, UML diagrams, documentation. Each member writes tests for their own module and
reviews one other member's pull requests.

**Git workflow:** `main` always runs. Work on `feature/<short-name>` branches, open a pull request, one
teammate reviews, then merge. Never commit `.env`.

## 4. Risk register

| # | Risk | Likelihood / Impact | Mitigation |
|---|---|---|---|
| R1 | Public OSRM server is demo-only, rate-limited, may be unavailable | High / Medium | Cache estimates; compute at approval time only; straight-line fallback; optional self-hosted OSRM |
| R2 | OSM tile / Nominatim policies (attribution, low volume) | Medium / Low | Attribution shown; pin-drop instead of geocoding; re-read policies before Phase 6 |
| R3 | No free transit routing for Mumbai | Certain / Medium | Admin-maintained station table |
| R4 | Free hosting tiers change or disappear | High / Medium | Local-first demo; hosting optional in Phase 10 |
| R5 | Scope too large for 3 people in one semester | High / High | Vertical slices; demo-first MVP; stretch items marked |
| R6 | AI-generated code the team cannot explain | Medium / High | Small increments, explanations per step, module ownership |
| R7 | Java version incompatibility | Medium / Medium | JDK 25 LTS; project compiles to Java 25 even on newer JDKs |
| R8 | Sample data mistaken for real listings | Low / Medium | `is_sample_data` flag and visible badge |
| R9 | Weights questioned as arbitrary | Medium / Medium | Documented rationale; optional student survey |
| R10 | Slow or unreliable internet when downloading tools | Medium / Low | Resumable downloads; Maven/npm caches; document offline steps |

## 5. Testing strategy

| Level | Tooling | Focus |
|---|---|---|
| Unit | JUnit 5 | Suitability and compatibility scorers, lifecycle transitions, fare maths |
| Integration | Spring Boot Test + Testcontainers (PostgreSQL 18) | Repositories, services, migrations |
| Security | MockMvc + spring-security-test | Role × endpoint matrix; ownership (IDOR) tests; 401 vs 403 |
| API | Bruno collection (in repo) | Normal, boundary, invalid, unauthorized cases |
| Frontend | ESLint, TypeScript; Vitest + Testing Library for key components | Forms, score display |
| End to end | Playwright | Full demo journey |

Rule: a feature is complete only when its acceptance test runs and passes. Results are recorded in
[STATUS.md](STATUS.md); nothing is reported as passing unless it was executed.
