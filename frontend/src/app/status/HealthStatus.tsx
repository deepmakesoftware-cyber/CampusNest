"use client";

import { useEffect, useState } from "react";

// Shape of GET /api/v1/health (see backend HealthController.HealthResponse).
type HealthResponse = {
  status: "UP" | "DEGRADED";
  database: "UP" | "DOWN";
  checkedAt: string;
};

type State =
  | { kind: "loading" }
  | { kind: "ok"; health: HealthResponse }
  | { kind: "error"; message: string };

export default function HealthStatus() {
  const [state, setState] = useState<State>({ kind: "loading" });

  useEffect(() => {
    // Relative URL: the request goes to Next.js, which forwards it to Spring Boot.
    fetch("/api/v1/health", { cache: "no-store" })
      .then(async (res) => {
        // A 503 still carries a JSON body describing what is down.
        if (res.ok || res.status === 503) {
          setState({ kind: "ok", health: (await res.json()) as HealthResponse });
        } else {
          setState({ kind: "error", message: `Backend answered with HTTP ${res.status}.` });
        }
      })
      .catch(() =>
        setState({ kind: "error", message: "Could not reach the backend. Is it running on port 8080?" }),
      );
  }, []);

  if (state.kind === "loading") {
    return <p className="text-slate-600">Checking services…</p>;
  }

  if (state.kind === "error") {
    return (
      <p role="alert" className="text-red-700">
        {state.message}
      </p>
    );
  }

  const { health } = state;
  return (
    <dl className="grid grid-cols-[auto_1fr] gap-x-6 gap-y-2">
      <dt className="text-slate-600">Frontend</dt>
      <dd className="font-medium text-green-700">UP</dd>
      <dt className="text-slate-600">Backend API</dt>
      <dd className={health.status === "UP" ? "font-medium text-green-700" : "font-medium text-amber-700"}>
        {health.status}
      </dd>
      <dt className="text-slate-600">Database</dt>
      <dd className={health.database === "UP" ? "font-medium text-green-700" : "font-medium text-red-700"}>
        {health.database}
      </dd>
      <dt className="text-slate-600">Checked at</dt>
      <dd>{new Date(health.checkedAt).toLocaleString()}</dd>
    </dl>
  );
}
