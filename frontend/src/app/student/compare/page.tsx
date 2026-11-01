import type { Metadata } from "next";
import { Suspense } from "react";
import RoleGate from "@/components/RoleGate";
import CompareView from "./CompareView";

export const metadata: Metadata = {
  title: "Compare cost and commute · CampusNest",
};

// The listing ids come from the URL (?ids=1,2,3), read with useSearchParams inside <Suspense>.
export default function ComparePage() {
  return (
    <main className="mx-auto w-full max-w-6xl px-4 py-10">
      <RoleGate allow={["STUDENT"]}>
        <Suspense fallback={<p className="text-slate-600">Loading…</p>}>
          <CompareView />
        </Suspense>
      </RoleGate>
    </main>
  );
}
