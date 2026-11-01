import type { Metadata } from "next";
import { Suspense } from "react";
import RoleGate from "@/components/RoleGate";
import ListingEditor from "./ListingEditor";

export const metadata: Metadata = {
  title: "Edit listing · CampusNest",
};

// The listing id comes from the URL and is read in the client component (useParams).
// With Cache Components enabled, that read must sit inside <Suspense>.
export default function EditListingPage() {
  return (
    <main className="mx-auto w-full max-w-3xl px-4 py-10">
      <RoleGate allow={["OWNER"]}>
        <Suspense fallback={<p className="text-slate-600">Loading…</p>}>
          <ListingEditor />
        </Suspense>
      </RoleGate>
    </main>
  );
}
