# CampusNest

**Smart Student Accommodation and Roommate Matching System** — FY MCA Mini Project-I, VJTI Mumbai.

CampusNest helps students find affordable and suitable accommodation near their colleges. It brings property
discovery, commute and cost comparison, and roommate matching together in one place, making it easier for
students moving to Mumbai to find a place they can call home.

For students who did not get a hostel seat, it ranks listings with an explainable suitability score, compares
rent against commute time and cost, makes listing trust visible, and matches compatible roommates without
exposing private details.

> **Development status:** all planned phases (0–10) complete: accounts and roles, listings with admin review, search
> and map, private enquiries, explainable suitability score, cost-vs-commute comparison, roommate matching, reviews,
> reports and owner verification, with 165 automated backend tests. See [docs/STATUS.md](docs/STATUS.md) for what is
> implemented and tested, and the known limitations.

## Tech stack

| Layer | Technology |
|---|---|
| Frontend | Next.js 16, React 19, TypeScript, Tailwind CSS 4 |
| Backend | Java 25 (LTS), Spring Boot 4.1, Spring Security, Spring Data JPA |
| Database | PostgreSQL 18 (Docker), Flyway migrations |
| Tests | JUnit 5, Spring Boot Test, Testcontainers |

## Repository layout

```
CampusNest/
├── backend/            Spring Boot API (Maven wrapper included)
├── frontend/           Next.js web app
├── docs/               Requirements, architecture, database, algorithms, plan, status
├── docker-compose.yml  Local PostgreSQL only
└── .env.example        Template for local settings (copy to .env)
```

## Prerequisites (one-time)

| Tool | Version | Check |
|---|---|---|
| JDK | 25 LTS (Microsoft Build of OpenJDK or Eclipse Temurin) | `java -version` |
| Node.js | 24 LTS | `node -v` |
| Docker Desktop | any recent | `docker info` |
| Git | any recent | `git --version` |

Maven does **not** need to be installed: the backend uses the Maven Wrapper (`mvnw`), which downloads it.

If you have several JDKs, point `JAVA_HOME` at JDK 25 for the terminal you run the backend in:

```powershell
# PowerShell (adjust the folder to where your JDK 25 is)
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\jdk-25.0.5.0.2+1"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

## First-time setup

```powershell
# 1. Create your local settings file and set a password of your choice in it
Copy-Item .env.example .env

# 2. Install frontend dependencies
cd frontend
npm install
cd ..
```

## Running locally (three terminals)

```powershell
# Terminal 1 — database (from the repo root). Docker Desktop must be running.
docker compose up -d

# Terminal 2 — backend API on http://localhost:8080
cd backend
.\mvnw.cmd spring-boot:run          # macOS/Linux: ./mvnw spring-boot:run

# Terminal 3 — frontend on http://localhost:3000
cd frontend
npm run dev
```

Open <http://localhost:3000/status>. All three rows (Frontend, Backend API, Database) should show **UP**.

### Logging in

- **Students and owners:** create an account at <http://localhost:3000/register>.
- **Administrator:** there is no admin sign-up. On startup the backend creates the admin account from
  `ADMIN_EMAIL` / `ADMIN_PASSWORD` in your `.env` (password at least 12 characters). Log in with those values.
  Changing them later does not change an existing account.

### Demo data (for presentations)

Set `DEMO_DATA=true` and a `DEMO_PASSWORD` (12+ characters) in `.env`, then restart the backend. It creates, once:

- 12 **sample listings** around VJTI (Matunga, Dadar, Sion, … Thane), each marked "Sample listing" with a
  generated "SAMPLE PHOTO" image. Rents are illustrative, not market data.
- 4 sample owners: `owner.ramesh@example.com`, `owner.sunita@example.com` (identity checked),
  `owner.imran@example.com`, `owner.prakash@example.com`
- 1 main sample student: `student.asha@example.com` (VJTI, MCA year 1) with accommodation preferences and a
  roommate profile
- 4 more sample students with roommate profiles: `student.meera@`, `student.priya@`, `student.kavya@`,
  `student.rohan@example.com`

All sample accounts use `DEMO_PASSWORD`. Never put real people's details in demo data.

How requests flow: the browser only talks to Next.js. Next.js forwards every `/api/*` request to Spring Boot
(see `frontend/next.config.ts`), and Spring Boot talks to PostgreSQL.

## Running tests

```powershell
# Backend (needs Docker running: tests start their own temporary PostgreSQL container)
cd backend
.\mvnw.cmd test

# Frontend lint and type-check
cd frontend
npm run lint
npx tsc --noEmit
```

## Useful commands

| Task | Command |
|---|---|
| Stop the database (keeps data) | `docker compose down` |
| Delete all local data | `docker compose down -v` |
| Open a SQL shell | `docker exec -it campusnest-db psql -U campusnest -d campusnest` |

## Security notes

- `.env` holds local passwords and is git-ignored. Only `.env.example` (placeholders) is committed.
- All API endpoints require login unless explicitly marked public in `SecurityConfig.java`.
- Never put real student data in sample or seed data.

## Documentation

- [Product requirements](docs/PRD.md)
- [Architecture & security](docs/ARCHITECTURE.md)
- [Database design](docs/DATABASE.md)
- [Scoring algorithms & listing lifecycle](docs/ALGORITHMS.md)
- [Project plan, risks & team split](docs/PROJECT-PLAN.md)
- [Status: completed / pending / known issues](docs/STATUS.md)
- [Feature backlog: agreed features for later](docs/BACKLOG.md)
- [REST API conventions and error codes](docs/API.md)
- [UML diagrams](docs/UML.md)
- [Test cases by requirement](docs/TEST-CASES.md)
- [User manual](docs/USER-MANUAL.md)
- [Faculty demonstration script](docs/DEMO-SCRIPT.md)
- [Deployment guide](docs/DEPLOYMENT.md)
- [Viva preparation](docs/VIVA.md)
