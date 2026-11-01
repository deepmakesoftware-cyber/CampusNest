import { ROOM_TYPE_LABELS, formatRupees } from "@/lib/listingLabels";
import type { Exclusion, SuitabilityResult } from "@/lib/types";

// Shows WHY a listing got its suitability score: each part, its weight, its score and a plain-English reason.
// A number alone is not an explanation (faculty requirement FR-1).

const EXCLUSION_TEXT: Record<Exclusion, string> = {
  FULL: "No free beds right now.",
  GENDER_POLICY: "This accommodation is not open to your gender.",
  OVER_BUDGET: "Every room costs more than 30% over your budget.",
};

export function ScoreBadge({ score, band }: { score: number; band: string }) {
  const colour = score >= 85 ? "bg-teal-700" : score >= 70 ? "bg-teal-600" : score >= 50 ? "bg-amber-600" : "bg-slate-500";
  return (
    <div className="flex items-center gap-2">
      <span className={`inline-flex h-12 w-12 items-center justify-center rounded-full text-lg font-bold text-white ${colour}`} aria-hidden="true">
        {score}
      </span>
      <span className="text-sm">
        <span className="block font-semibold text-slate-900">{band}</span>
        <span className="text-slate-500">Suitability score {score}/100</span>
      </span>
    </div>
  );
}

export default function ScoreBreakdown({ result }: { result: SuitabilityResult }) {
  if (!result.eligible) {
    return (
      <div className="text-sm text-slate-700">
        <p className="font-medium text-slate-900">Not recommended for your preferences</p>
        <ul className="mt-1 list-disc pl-5">
          {result.exclusions.map((e) => <li key={e}>{EXCLUSION_TEXT[e]}</li>)}
        </ul>
      </div>
    );
  }
  return (
    <div className="space-y-3 text-sm">
      {result.bestRoomType && result.monthlyCost !== null && (
        <p className="text-slate-700">
          Scored for the best-fitting room: <span className="font-medium">{ROOM_TYPE_LABELS[result.bestRoomType]}</span>,{" "}
          {formatRupees(result.monthlyCost)} per month including mandatory charges.
        </p>
      )}
      <ul className="space-y-3">
        {result.components.map((c) => (
          <li key={c.key}>
            <div className="flex items-baseline justify-between gap-2">
              <span className="font-medium text-slate-900">
                {c.label} <span className="font-normal text-slate-500">({c.weight}% of total)</span>
              </span>
              <span className="tabular-nums text-slate-700">{c.score}/100</span>
            </div>
            <div className="mt-1 h-2 rounded-full bg-slate-100" role="img" aria-label={`${c.label}: ${c.score} out of 100`}>
              <div className={`h-2 rounded-full ${c.score >= 70 ? "bg-teal-600" : c.score >= 40 ? "bg-amber-500" : "bg-red-500"}`} style={{ width: `${c.score}%` }} />
            </div>
            <p className="mt-1 text-slate-600">{c.explanation}</p>
          </li>
        ))}
      </ul>
      <p className="text-xs text-slate-500">
        Total = sum of (weight × part score) ÷ 100 ={" "}
        {result.components.map((c) => `${c.weight}×${c.score}`).join(" + ")} ÷ 100 = {result.totalScore}.
        Data completeness: {result.dataCompleteness}% (how much of what matters to you is known rather than estimated).
      </p>
      {result.notes.length > 0 && (
        <ul className="list-disc pl-5 text-xs text-slate-500">
          {result.notes.map((n) => <li key={n}>{n}</li>)}
        </ul>
      )}
    </div>
  );
}
