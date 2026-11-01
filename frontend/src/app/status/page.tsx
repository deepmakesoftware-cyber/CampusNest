import type { Metadata } from "next";
import HealthStatus from "./HealthStatus";

export const metadata: Metadata = {
  title: "System status · CampusNest",
};

// Developer page proving the chain browser -> Next.js -> Spring Boot -> PostgreSQL works.
export default function StatusPage() {
  return (
    <main className="mx-auto w-full max-w-xl px-4 py-12">
      <h1 className="text-2xl font-semibold text-slate-900">System status</h1>
      <p className="mt-2 text-slate-600">Live check of each part of the CampusNest stack.</p>
      <section className="mt-8 rounded-lg border border-slate-200 bg-white p-6">
        <HealthStatus />
      </section>
    </main>
  );
}
