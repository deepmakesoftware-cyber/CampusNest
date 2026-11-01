import type { Metadata } from "next";
import { Suspense } from "react";
import RoleGate from "@/components/RoleGate";
import AdminListingReview from "./AdminListingReview";

export const metadata: Metadata = {
  title: "Review listing · CampusNest admin",
};

export default function AdminListingReviewPage() {
  return (
    <main className="mx-auto w-full max-w-5xl px-4 py-10">
      <RoleGate allow={["ADMIN"]}>
        <Suspense fallback={<p className="text-slate-600">Loading…</p>}>
          <AdminListingReview />
        </Suspense>
      </RoleGate>
    </main>
  );
}
