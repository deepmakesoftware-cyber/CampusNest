"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { CompareToggle } from "@/components/CompareControls";
import ScoreBreakdown, { ScoreBadge } from "@/components/ScoreBreakdown";
import { OwnerCheckedBadge, SampleBadge } from "@/components/TrustBadges";
import { FormMessage } from "@/components/form";
import { ApiError, api, errorMessage } from "@/lib/api";
import { GENDER_POLICY_LABELS, PROPERTY_TYPE_LABELS, ROOM_TYPE_LABELS, formatRupees } from "@/lib/listingLabels";
import type { Recommendations } from "@/lib/types";

export default function RecommendationList() {
  const [data, setData] = useState<Recommendations | null>(null);
  const [missing, setMissing] = useState<"COLLEGE_REQUIRED" | "PREFERENCES_REQUIRED" | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [open, setOpen] = useState<number | null>(null);

  useEffect(() => {
    api<Recommendations>("/api/v1/student/recommendations")
      .then(setData)
      .catch((e) => {
        if (e instanceof ApiError && (e.code === "COLLEGE_REQUIRED" || e.code === "PREFERENCES_REQUIRED")) {
          setMissing(e.code);
        } else {
          setError(errorMessage(e));
        }
      });
  }, []);

  if (missing) {
    return (
      <div className="rounded-lg border border-dashed border-slate-300 bg-white p-8 text-center">
        <h1 className="text-xl font-semibold text-slate-900">Tell us a little first</h1>
        <p className="mt-2 text-slate-600">
          {missing === "COLLEGE_REQUIRED"
            ? "Choose your college so we can estimate your commute."
            : "Set your budget, commute and room preferences so we can score listings for you."}
        </p>
        <Link href={missing === "COLLEGE_REQUIRED" ? "/account" : "/student/preferences"} className="mt-4 inline-block rounded-md bg-teal-700 px-4 py-2 font-medium text-white">
          {missing === "COLLEGE_REQUIRED" ? "Add my college" : "Set my preferences"}
        </Link>
      </div>
    );
  }
  if (error) return <FormMessage kind="error">{error}</FormMessage>;
  if (!data) return <p className="text-slate-600">Scoring listings for you…</p>;

  const excluded = Object.entries(data.excludedByReason);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">Recommended for you</h1>
          <p className="mt-1 text-slate-600">
            Ranked by suitability score for your preferences, with commutes to {data.collegeShortName}.
          </p>
        </div>
        <Link href="/student/preferences" className="text-sm text-teal-700 underline underline-offset-4">Change preferences</Link>
      </div>

      <details className="rounded-lg border border-slate-200 bg-white p-4 text-sm text-slate-700">
        <summary className="cursor-pointer font-medium text-slate-900">How is the score calculated?</summary>
        <p className="mt-2">
          Every listing gets five part-scores out of 100: <b>budget</b> (30%), <b>commute</b> (25%), <b>room type</b> (15%),{" "}
          <b>amenities</b> (15%) and <b>trust</b> (15%). The total is their weighted sum. Listings with no free beds, not open to
          your gender, or more than 30% over your budget are left out. Commute times are estimates. The score is a guide to help you
          compare. It does not guarantee the quality or safety of a place, so always visit first.
        </p>
      </details>

      {excluded.length > 0 && (
        <p className="text-sm text-slate-600">
          Not shown: {excluded.map(([reason, count]) => `${count} × ${reason.toLowerCase()}`).join("; ")}.
        </p>
      )}

      {data.items.length === 0 ? (
        <div className="rounded-lg border border-dashed border-slate-300 bg-white p-8 text-center text-slate-600">
          No listing fits right now. Try a higher budget or a longer commute in your preferences.
        </div>
      ) : (
        <ol className="space-y-4">
          {data.items.map((item, index) => {
            const r = item.result;
            return (
              <li key={item.listingId} className="rounded-lg border border-slate-200 bg-white p-4">
                <div className="flex flex-col gap-4 sm:flex-row">
                  {item.coverImageUrl && (
                    // eslint-disable-next-line @next/next/no-img-element -- photos are served by our own API
                    <img src={item.coverImageUrl} alt="" className="aspect-[4/3] w-full rounded-md object-cover sm:w-40" />
                  )}
                  <div className="flex-1 space-y-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="text-sm text-slate-500">#{index + 1}</span>
                      <Link href={`/listings/${item.listingId}`} className="font-semibold text-slate-900 hover:underline">{item.title}</Link>
                      {item.sampleData && <SampleBadge />}
                      <OwnerCheckedBadge status={item.ownerVerification} />
                      <CompareToggle listingId={item.listingId} />
                    </div>
                    <p className="text-sm text-slate-600">
                      {item.locality} · {PROPERTY_TYPE_LABELS[item.propertyType]} · {GENDER_POLICY_LABELS[item.genderPolicy]}
                    </p>
                    <p className="text-sm text-slate-700">
                      {r.bestRoomType && ROOM_TYPE_LABELS[r.bestRoomType]} · {r.monthlyCost !== null && formatRupees(r.monthlyCost)}/month
                      {r.commute && ` · about ${r.commute.minutes} min to ${data.collegeShortName}${r.commute.approximate ? " (estimate)" : ""}`}
                    </p>
                  </div>
                  {r.totalScore !== null && r.band && <ScoreBadge score={r.totalScore} band={r.band} />}
                </div>
                <button
                  type="button"
                  onClick={() => setOpen(open === item.listingId ? null : item.listingId)}
                  aria-expanded={open === item.listingId}
                  className="mt-3 text-sm text-teal-700 underline underline-offset-4"
                >
                  {open === item.listingId ? "Hide score breakdown" : "Why this score?"}
                </button>
                {open === item.listingId && (
                  <div className="mt-3 border-t border-slate-100 pt-3">
                    <ScoreBreakdown result={r} />
                  </div>
                )}
              </li>
            );
          })}
        </ol>
      )}
    </div>
  );
}
