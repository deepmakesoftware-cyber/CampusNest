# CampusNest — Deployment Guide

CampusNest is designed to **run fully on one laptop** (the configuration used for the faculty demonstration). This
guide covers local use, building release artefacts, and what must change before any public deployment.

## 1. Local (development and demonstration)

See the [README](../README.md): `docker compose up -d`, `.\mvnw.cmd spring-boot:run`, `npm run dev`.
Requirements: JDK 25, Node.js 24, Docker Desktop. No paid service, API key or credit card is needed.

## 2. Building release artefacts

```powershell
# Backend: a single runnable jar (tests run first)
cd backend
.\mvnw.cmd clean package
java -jar target\campusnest-backend-0.0.1-SNAPSHOT.jar      # reads ../.env or real environment variables

# Frontend: optimised production build
cd frontend
npm ci
npm run build
npm run start            # serves on port 3000; BACKEND_URL must point at the backend
```

## 3. Configuration (environment variables)

| Variable | Purpose | Production value |
|---|---|---|
| `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Database connection | Managed or self-hosted PostgreSQL 18; strong password |
| `BACKEND_PORT` | Backend port | 8080 behind a reverse proxy |
| `FRONTEND_ORIGIN` | CORS allow-list | The public site URL, e.g. `https://campusnest.example` |
| `SESSION_COOKIE_SECURE` | Cookie only over HTTPS | **`true`** |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_DISPLAY_NAME` | First admin account (created once) | Real admin email; 16+ character password; remove from the environment after first start if desired |
| `STORAGE_DIR` | Folder for listing photos | A persistent, backed-up volume |
| `DEMO_DATA`, `DEMO_PASSWORD` | Sample data | **`false`** in production |
| `BACKEND_URL` (frontend) | Where Next.js forwards `/api/*` | Internal backend URL |
| `NEXT_PUBLIC_MAP_TILE_URL` (frontend) | Map tile server | A tile provider whose usage policy allows your traffic |

## 4. Checklist before a public deployment

- [ ] HTTPS everywhere (reverse proxy such as Caddy or Nginx with Let's Encrypt), `SESSION_COOKIE_SECURE=true`.
- [ ] `DEMO_DATA=false`; database started empty; Flyway applies migrations V1–V7 automatically.
- [ ] Backups of the PostgreSQL database **and** the photo folder (`STORAGE_DIR`), and a tested restore.
- [ ] Hide or protect the developer `/status` page (known issue T4).
- [ ] Login and request limits are per server, in memory (T8): run one backend instance, or move counters to the
      database/Redis before scaling out.
- [ ] Map tiles: the public OpenStreetMap server is for light use only; switch `NEXT_PUBLIC_MAP_TILE_URL` to a
      provider whose terms fit the expected traffic. Keep the attribution visible.
- [ ] Enter verified local-train pass fares at `/admin/fares` (T21) or explain to users why train costs are missing.
- [ ] Privacy policy and terms of use (contact sharing, reviews, reports, data deletion requests).
- [ ] Log monitoring and disk-space alerts for the photo folder.

## 5. Hosting options and caveats

Free hosting tiers change often and may sleep, delete idle databases, or require a card. CampusNest therefore does
**not** depend on any of them. If the team wants a public test deployment later:

- One small VM (student cloud credits or a college server) running Docker Compose with PostgreSQL, the backend jar
  and the Next.js server behind Caddy is the simplest option to explain and maintain.
- Re-check each provider's current limits and terms before relying on it, and keep the local setup working as the
  fallback for demonstrations.
