import type { Metadata } from "next";
import ListingSearch from "./ListingSearch";

export const metadata: Metadata = {
  title: "Find accommodation · CampusNest",
};

export default function ListingsPage() {
  return (
    <main className="mx-auto w-full max-w-6xl px-4 py-8">
      <h1 className="text-2xl font-semibold text-slate-900">Find accommodation</h1>
      <p className="mt-1 text-slate-600">Reviewed listings near your college. Distances are straight-line estimates.</p>
      <ListingSearch />
    </main>
  );
}
