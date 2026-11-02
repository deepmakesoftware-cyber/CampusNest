# CampusNest — Faculty Demonstration Script (about 15 minutes)

Covers all nine demonstration requirements using **working functionality only**. Sample data is clearly labelled
"Sample listing" on screen; say so when presenting.

## Before the review (10 minutes, the day before and again on the morning)

1. Start Docker Desktop, then in three terminals (see [README](../README.md)):
   `docker compose up -d` · backend `.\mvnw.cmd spring-boot:run` · frontend `npm run dev`.
2. Check `.env` has `DEMO_DATA=true` and a `DEMO_PASSWORD`; note the admin and demo passwords on paper.
3. Run `bash scripts/smoke-test.sh` from the repository root → must end with `0 failure(s)`.
4. Open three browser windows (or one normal + two private windows) so each role stays logged in:
   **A** student · **B** owner · **C** admin. Zoom to ~110% for the projector.
5. Optional backup: run `cd backend && .\mvnw.cmd test` once and keep the "Tests run: 165, Failures: 0" output open.

Accounts (all sample accounts use `DEMO_PASSWORD`):

| Role | Email |
|---|---|
| Student | `student.asha@example.com` (preferences and roommate profile ready) |
| Owner | `owner.ramesh@example.com` (identity checked), `owner.imran@example.com` (not checked) |
| Admin | value of `ADMIN_EMAIL` in `.env` |
| New student for requirement 1 | register live, e.g. `demo.student1@example.com` |

## The script

| # | Requirement | Window | Steps | Say |
|---|---|---|---|---|
| 0 | Problem | — | Show homepage | "Students who miss hostel seats search scattered sources; CampusNest ranks, explains, compares and protects privacy." |
| 1 | **Student registers and enters preferences** | A (log out first) | Sign up → Student → name/email/password → Account: college VJTI, MCA, year 1 → Recommended → "Set my preferences": ₹11,000, 40 min, Double first, Wi-Fi must-have | "Passwords are BCrypt-hashed; the session cookie is HttpOnly." |
| 2 | **Multiple options displayed** | A | Find accommodation → list and **Map** views, filters (rent, women-only, Wi-Fi), distances from VJTI | "Only listings an admin approved are shown. Distances are straight-line, and labelled so." |
| 3 | **Suitability scores calculated** | A | Recommended → open "Why this score?" on the top result | "Five parts with weights 30/25/15/15/15; each has a reason; the total is the weighted sum shown underneath." |
| 4 | **Recommendations ranked** | A (as Asha) | Point at rank order and the "Not shown" line (for Asha: 2 × not open to your gender, 1 × no free beds) | "Excluded listings are counted, not silently hidden." |
| 5 | **Cost-versus-commute comparison** | A | "+ Compare" on Green Nest PG and Chembur Homestay (and Thane) → Compare cost and commute → switch "Assume travel by" | "Chembur is cheaper rent but the commute costs more; Thane's total is *Incomplete* because no verified train-pass fare exists. Estimates are never shown as confirmed costs." |
| 6 | **Roommate match with score** | A (log in as Asha) | Roommates → Meera 87%: in common / differences → "Ask to connect" | "Only opted-in students see each other, as 'Meera J.'. Contact details appear only after both accept." Optional: log in as `student.meera@example.com` in B and accept, then show Connections. |
| 7 | **Owner listing workflow** | B (owner.imran) | My listings → create "Demo PG Dadar" → fill basics, drop map pin, add room (₹9,000, 4 beds, 2 free), answer amenities, upload a photo → Save and submit | "The checklist blocks incomplete submissions. Photos are re-encoded so GPS data is removed." |
| 8 | **Administrator verification workflow** | C | Listing review → open the new listing → **Request changes** ("add landmark") → B: edit + resubmit → C: **Approve** → show History. Then Owners → verify Imran (Phone call, note) | "Every status change is recorded with who and why. 'Identity checked' is about the owner, not a safety guarantee." |
| 9 | **Privacy and role-based access** | A + C | In A (student) open `http://localhost:3000/admin/listings` → "not available to you". Then show the backend refuses too: run `bash scripts/smoke-test.sh` (lines `student cannot use admin API … ok`) or show `RoleAccessTest` | "Hiding a button is not security: the API itself returns 403/401." |
| — | Trust extras (if time) | A/C | Listing page: write a review (after enquiry), report a listing; C: Reports → uphold → that listing loses the 10 "no upheld reports" points in its trust part | "Reviews are labelled 'stay not verified'." |

## If something goes wrong

- Page shows "Could not reach the backend": check terminal 2; restart `.\mvnw.cmd spring-boot:run`.
- Map tiles don't load (no internet): coordinates can still be typed; say tiles come from OpenStreetMap.
- Login says too many attempts: wait 15 minutes or restart the backend (counters are in memory).
- Fallback evidence: `docs/TEST-CASES.md` and the test run output.

## After the demo (reset sample state)

Demo actions (new listing, connections, reviews) stay in the local database. To start fresh:
`docker compose down -v` then start everything again; the admin and all sample data are recreated automatically.
**This deletes all local data**, including any accounts created by hand.

## Screenshots for the report

Take these from the real running app (Windows: Win + Shift + S): homepage · search list + map · recommendation
with breakdown open · comparison table + chart · roommate match card · owner listing editor with checklist ·
admin review page with history · admin reports queue · "not available to you" page.
