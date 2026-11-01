"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/lib/auth";
import { MAX_COMPARE, useCompareSelection } from "@/lib/compare";

/** "+ Compare" toggle shown on listing cards and pages (students only). */
export function CompareToggle({ listingId }: { listingId: number }) {
  const { state } = useAuth();
  const { has, ids, toggle } = useCompareSelection();
  if (state.status !== "authenticated" || state.user.role !== "STUDENT") return null;
  const selected = has(listingId);
  const full = !selected && ids.length >= MAX_COMPARE;
  return (
    <button
      type="button"
      onClick={(e) => {
        e.preventDefault(); // cards are links; don't navigate
        toggle(listingId);
      }}
      disabled={full}
      aria-pressed={selected}
      title={full ? `You can compare up to ${MAX_COMPARE} listings` : undefined}
      className={`rounded-md border px-2 py-1 text-xs font-medium ${
        selected ? "border-teal-700 bg-teal-700 text-white" : "border-slate-300 bg-white text-slate-700 hover:border-teal-600"
      } disabled:opacity-50`}
    >
      {selected ? "✓ Comparing" : "+ Compare"}
    </button>
  );
}

/** Bar at the bottom of the screen while a student has a comparison shortlist. */
export function CompareBar() {
  const { state } = useAuth();
  const { ids, clear } = useCompareSelection();
  const pathname = usePathname();
  if (state.status !== "authenticated" || state.user.role !== "STUDENT" || ids.length === 0 || pathname === "/student/compare") {
    return null;
  }
  return (
    <>
    {/* spacer so the fixed bar never covers the end of the page */}
    <div className="h-20" aria-hidden="true" />
    <div className="fixed inset-x-0 bottom-0 z-20 border-t border-slate-200 bg-white/95 px-4 py-3 shadow-lg backdrop-blur">
      <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-3 text-sm">
        <span className="text-slate-700">
          {ids.length} of {MAX_COMPARE} listings selected to compare
        </span>
        <div className="flex items-center gap-3">
          <button type="button" onClick={clear} className="text-slate-600 underline underline-offset-4">Clear</button>
          {ids.length >= 2 ? (
            <Link href={`/student/compare?ids=${ids.join(",")}`} className="rounded-md bg-teal-700 px-3 py-1.5 font-medium text-white">
              Compare cost and commute
            </Link>
          ) : (
            <span className="text-slate-500">Select at least 2</span>
          )}
        </div>
      </div>
    </div>
    </>
  );
}
