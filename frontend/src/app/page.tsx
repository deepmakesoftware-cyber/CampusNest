import Link from "next/link";

// Placeholder homepage for Phase 1. The real homepage is designed in later phases.
export default function Home() {
  return (
    <main className="mx-auto flex w-full max-w-2xl flex-1 flex-col justify-center px-4 py-16">
      <p className="text-sm font-medium uppercase tracking-wide text-teal-700">CampusNest</p>
      <h1 className="mt-2 text-3xl font-semibold text-slate-900 sm:text-4xl">
        Find student accommodation that fits your budget, commute and lifestyle.
      </h1>
      <p className="mt-4 text-slate-600">
        Ranked, explained recommendations for students in Mumbai, starting with VJTI.
        This is an early development build: features are being added phase by phase.
      </p>
      <Link href="/status" className="mt-8 text-teal-700 underline underline-offset-4 hover:text-teal-900">
        View system status
      </Link>
    </main>
  );
}
