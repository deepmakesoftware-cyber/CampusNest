import HomeActions from "./HomeActions";

// Homepage. The full design (search entry point, how scoring works) comes with Phases 4–5.
export default function Home() {
  return (
    <main className="mx-auto flex w-full max-w-2xl flex-1 flex-col justify-center px-4 py-16">
      <p className="text-sm font-medium uppercase tracking-wide text-teal-700">For students in Mumbai</p>
      <h1 className="mt-2 text-3xl font-semibold text-slate-900 sm:text-4xl">
        Find student accommodation that fits your budget, commute and lifestyle.
      </h1>
      <p className="mt-4 text-slate-600">
        CampusNest ranks accommodation near your college, explains every score, compares rent against travel time,
        and helps you find a compatible roommate — starting with VJTI.
      </p>
      <div className="mt-8">
        <HomeActions />
      </div>
      <p className="mt-10 text-xs text-slate-500">
        Early development build. Some features are still being added.
      </p>
    </main>
  );
}
