import type { Metadata } from "next";
import { Suspense } from "react";
import PublicListingPage from "./PublicListingPage";

export const metadata: Metadata = {
  title: "Listing · CampusNest",
};

export default function ListingPage() {
  return (
    <main className="mx-auto w-full max-w-5xl px-4 py-8">
      <Suspense fallback={<p className="text-slate-600">Loading…</p>}>
        <PublicListingPage />
      </Suspense>
    </main>
  );
}
