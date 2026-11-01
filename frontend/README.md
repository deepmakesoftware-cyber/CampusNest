# CampusNest frontend

Next.js 16 (App Router) + React 19 + TypeScript + Tailwind CSS 4.

See the [root README](../README.md) for full setup. Quick start:

```powershell
npm install
npm run dev      # http://localhost:3000  (backend must run on :8080)
npm run lint
```

- All `/api/*` requests are forwarded to the Spring Boot backend by `next.config.ts`.
- This Next.js version has breaking changes from older tutorials; check `node_modules/next/dist/docs/`
  (see `AGENTS.md`) before using an unfamiliar API.
