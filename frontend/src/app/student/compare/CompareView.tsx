"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import CostCommuteChart, { type ChartPoint } from "@/components/CostCommuteChart";
import { SampleBadge } from "@/components/TrustBadges";
import { FormMessage } from "@/components/form";
import { ApiError, api, errorMessage } from "@/lib/api";
import { useCompareSelection } from "@/lib/compare";
import { ROOM_TYPE_LABELS, formatDate, formatRupees } from "@/lib/listingLabels";
import type { CommuteOption, ComparedListing, Comparison, FareRuleView, TravelMode } from "@/lib/types";

const MODE_LABELS: Record<Exclude<TravelMode, "ANY">, string> = {
  WALK: "Walking",
  AUTO_RICKSHAW: "Auto-rickshaw",
  BUS: "BEST bus",
  LOCAL_TRAIN: "Local train",
};
const MODES = Object.keys(MODE_LABELS) as Exclude<TravelMode, "ANY">[];

type Choice = "SUGGESTED" | Exclude<TravelMode, "ANY">;

function chosenOption(item: ComparedListing, choice: Choice): CommuteOption | null {
  const mode = choice === "SUGGESTED" ? item.suggestedMode : choice;
  return item.commuteOptions.find((o) => o.mode === mode && o.available) ?? null;
}

/** Overall monthly cost for the chosen mode: only when every part is known (never a guess). */
function total(item: ComparedListing, option: CommuteOption | null): number | null {
  if (!option || option.monthlyCost === null) return null;
  return item.monthlyRent + item.compulsoryMonthlyCharges + option.monthlyCost;
}

export default function CompareView() {
  const params = useSearchParams();
  const ids = params.get("ids") ?? "";
  const { clear } = useCompareSelection();
  const [days, setDays] = useState("22");
  const [appliedDays, setAppliedDays] = useState(22);
  const [choice, setChoice] = useState<Choice>("SUGGESTED");
  const [data, setData] = useState<Comparison | null>(null);
  const [fares, setFares] = useState<FareRuleView[]>([]);
  const [error, setError] = useState<{ text: string; code?: string } | null>(null);

  useEffect(() => {
    if (!ids) return;
    api<Comparison>(`/api/v1/student/compare?ids=${encodeURIComponent(ids)}&daysPerMonth=${appliedDays}`)
      .then((d) => {
        setData(d);
        setError(null);
      })
      .catch((e) => setError({ text: errorMessage(e), code: e instanceof ApiError ? e.code : undefined }));
  }, [ids, appliedDays]);

  useEffect(() => {
    api<FareRuleView[]>("/api/v1/fare-rules").then(setFares).catch(() => setFares([]));
  }, []);

  const points: ChartPoint[] = useMemo(() => {
    if (!data) return [];
    return data.items.flatMap((item) => {
      const option = chosenOption(item, choice);
      const t = total(item, option);
      return option && t !== null
        ? [{ id: item.listingId, label: item.title, minutes: option.minutes, monthly: t, modeLabel: MODE_LABELS[option.mode as keyof typeof MODE_LABELS].toLowerCase() }]
        : [];
    });
  }, [data, choice]);

  if (!ids) {
    return (
      <div className="rounded-lg border border-dashed border-slate-300 bg-white p-8 text-center text-slate-600">
        Choose 2 to 4 listings with “+ Compare” on the search or recommendations page.{" "}
        <Link href="/listings" className="text-teal-700 underline underline-offset-4">Browse listings</Link>
      </div>
    );
  }
  if (error) {
    return (
      <div className="space-y-3">
        <FormMessage kind="error">{error.text}</FormMessage>
        {error.code === "COLLEGE_REQUIRED" && <Link href="/account" className="text-teal-700 underline underline-offset-4">Add your college</Link>}
      </div>
    );
  }
  if (!data) return <p className="text-slate-600">Comparing…</p>;

  const sources = Array.from(new Map(fares.map((f) => [f.sourceName, f])).values());

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">Cost vs commute</h1>
          <p className="mt-1 text-slate-600">
            Rent and charges come from the listings. Travel times and transport costs are <strong>estimates</strong> for trips to {data.collegeShortName}, not confirmed amounts.
          </p>
        </div>
        <button type="button" onClick={clear} className="text-sm text-slate-600 underline underline-offset-4">Clear shortlist</button>
      </div>

      <div className="flex flex-wrap items-end gap-4 rounded-lg border border-slate-200 bg-white p-4 text-sm">
        <div>
          <label htmlFor="mode" className="block font-medium text-slate-800">Assume travel by</label>
          <select id="mode" value={choice} onChange={(e) => setChoice(e.target.value as Choice)} className="mt-1 rounded-md border border-slate-300 bg-white px-2 py-1.5">
            <option value="SUGGESTED">Suggested (cheapest within your {data.maxCommuteMinutes}-min limit)</option>
            {MODES.map((m) => <option key={m} value={m}>{MODE_LABELS[m]}</option>)}
          </select>
        </div>
        <form onSubmit={(e) => { e.preventDefault(); const n = Number(days); if (n >= 1 && n <= 31) setAppliedDays(n); }}>
          <label htmlFor="days" className="block font-medium text-slate-800">College days per month</label>
          <div className="mt-1 flex gap-2">
            <input id="days" value={days} onChange={(e) => setDays(e.target.value)} inputMode="numeric" className="w-20 rounded-md border border-slate-300 px-2 py-1.5" />
            <button type="submit" className="rounded-md border border-slate-300 px-3">Update</button>
          </div>
        </form>
        <p className="text-slate-500">Each day counts as 2 trips (there and back).</p>
      </div>

      {points.length >= 1 && (
        <section className="rounded-lg border border-slate-200 bg-white p-4">
          <h2 className="font-semibold text-slate-900">Estimated monthly cost vs commute time</h2>
          <CostCommuteChart points={points} />
        </section>
      )}

      <section className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="min-w-full text-left text-sm">
          <caption className="sr-only">Comparison of selected listings</caption>
          <thead className="border-b border-slate-200 bg-slate-50 align-top">
            <tr>
              <th scope="col" className="w-48 px-4 py-3 font-medium text-slate-600"> </th>
              {data.items.map((item) => (
                <th key={item.listingId} scope="col" className="min-w-48 px-4 py-3">
                  <Link href={`/listings/${item.listingId}`} className="font-semibold text-slate-900 hover:underline">{item.title}</Link>
                  <div className="font-normal text-slate-600">{item.locality}</div>
                  {item.sampleData && <div className="mt-1"><SampleBadge /></div>}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            <Row label="Room compared">{data.items.map((i) => ROOM_TYPE_LABELS[i.roomType])}</Row>
            <Row label="Rent / month">{data.items.map((i) => formatRupees(i.monthlyRent))}</Row>
            <Row label="Compulsory monthly charges">{data.items.map((i) => (i.compulsoryMonthlyCharges ? formatRupees(i.compulsoryMonthlyCharges) : "None listed"))}</Row>
            <Row label="Optional monthly extras">{data.items.map((i) => (i.optionalMonthlyCharges ? formatRupees(i.optionalMonthlyCharges) : "—"))}</Row>
            <Row label="One-time charges">{data.items.map((i) => (i.oneTimeCharges ? formatRupees(i.oneTimeCharges) : "—"))}</Row>
            <Row label="Security deposit">{data.items.map((i) => (i.securityDeposit !== null ? formatRupees(i.securityDeposit) : "Not stated"))}</Row>
            <Row label="Straight-line distance">{data.items.map((i) => `${i.straightLineKm} km`)}</Row>

            {MODES.map((mode) => (
              <tr key={mode} className={choice === mode ? "bg-teal-50/40" : ""}>
                <th scope="row" className="px-4 py-3 font-medium text-slate-700">
                  {MODE_LABELS[mode]}
                  <span className="block text-xs font-normal text-slate-500">time · cost / month (estimated)</span>
                </th>
                {data.items.map((item) => {
                  const o = item.commuteOptions.find((x) => x.mode === mode);
                  const isSuggested = item.suggestedMode === mode;
                  return (
                    <td key={item.listingId} className="px-4 py-3 align-top">
                      {!o || !o.available ? (
                        <span className="text-slate-500">{o?.unavailableReason ?? "Not available"}</span>
                      ) : (
                        <details>
                          <summary className="cursor-pointer list-none">
                            <span className="font-medium text-slate-900">≈ {o.minutes} min</span>
                            <span className="text-slate-600"> · {o.monthlyCost !== null ? `≈ ${formatRupees(o.monthlyCost)}` : "cost n/a"}</span>
                            {isSuggested && <span className="ml-1 rounded bg-teal-50 px-1 text-xs text-teal-800">suggested</span>}
                          </summary>
                          <p className="mt-1 text-xs text-slate-500">{o.method}</p>
                          <p className="mt-1 text-xs text-slate-500">{o.costNote}</p>
                        </details>
                      )}
                    </td>
                  );
                })}
              </tr>
            ))}

            <tr className="bg-slate-50">
              <th scope="row" className="px-4 py-3 font-semibold text-slate-900">
                Estimated total / month
                <span className="block text-xs font-normal text-slate-500">rent + compulsory charges + travel</span>
              </th>
              {data.items.map((item) => {
                const option = chosenOption(item, choice);
                const t = total(item, option);
                return (
                  <td key={item.listingId} className="px-4 py-3 align-top">
                    {t !== null ? (
                      <>
                        <span className="text-lg font-semibold text-slate-900">≈ {formatRupees(t)}</span>
                        <span className="block text-xs text-slate-500">with {MODE_LABELS[option!.mode as keyof typeof MODE_LABELS].toLowerCase()}</span>
                      </>
                    ) : (
                      <span className="text-sm text-amber-800">
                        Incomplete — {option ? option.costNote : `${choice === "SUGGESTED" ? "no option" : MODE_LABELS[choice]} not available here`}
                      </span>
                    )}
                  </td>
                );
              })}
            </tr>
          </tbody>
        </table>
      </section>

      <section className="space-y-2 text-xs text-slate-500">
        <p>
          <strong className="text-slate-700">How these estimates are made.</strong> Road distance is the straight line × 1.3. Walking 4.5 km/h;
          auto-rickshaw 18 km/h + 5 min waiting; BEST bus 13 km/h + 10 min waiting (assumes a direct route; check before relying on it);
          local train: walk to the nearest station, about 8 min wait, rail at 30 km/h, walk from the station nearest your college.
          These are assumptions, not timetables.
        </p>
        <p>
          <strong className="text-slate-700">Fare sources.</strong>{" "}
          {sources.map((f, i) => (
            <span key={f.sourceName}>
              {i > 0 && "; "}
              {f.sourceUrl ? <a href={f.sourceUrl} target="_blank" rel="noopener noreferrer" className="underline">{f.sourceName}</a> : f.sourceName} (from {formatDate(f.effectiveFrom)})
            </span>
          ))}
          {fares.every((f) => f.mode !== "LOCAL_TRAIN") && ". Local-train pass prices have not been entered yet, so train costs show as not available."}
        </p>
        <p>Station locations © OpenStreetMap contributors (ODbL).</p>
      </section>
    </div>
  );
}

function Row({ label, children }: { label: string; children: React.ReactNode[] }) {
  return (
    <tr>
      <th scope="row" className="px-4 py-3 font-medium text-slate-700">{label}</th>
      {children.map((c, i) => <td key={i} className="px-4 py-3 text-slate-800">{c}</td>)}
    </tr>
  );
}
