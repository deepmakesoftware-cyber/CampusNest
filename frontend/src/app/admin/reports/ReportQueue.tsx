"use client";

import Link from "next/link";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import AdminOverview from "@/components/AdminOverview";
import { FormMessage } from "@/components/form";
import { api, errorMessage } from "@/lib/api";
import { formatDateTime } from "@/lib/listingLabels";
import type { Page, ReportView } from "@/lib/types";

const REASON_LABELS: Record<ReportView["reason"], string> = {
  FAKE_LISTING: "Fake listing",
  WRONG_INFORMATION: "Wrong information",
  UNSAFE: "Safety concern",
  SPAM: "Spam / scam",
  HARASSMENT: "Harassment",
  INAPPROPRIATE: "Inappropriate",
  OTHER: "Other",
};

export default function ReportQueue() {
  const [status, setStatus] = useState<"OPEN" | "UPHELD" | "DISMISSED">("OPEN");
  const [data, setData] = useState<Page<ReportView> | null>(null);
  const [message, setMessage] = useState<{ kind: "error" | "success"; text: string } | null>(null);

  const load = useCallback(() => {
    api<Page<ReportView>>(`/api/v1/admin/reports?status=${status}`).then(setData).catch((e) => setMessage({ kind: "error", text: errorMessage(e) }));
  }, [status]);
  useEffect(load, [load]);

  return (
    <div>
      <AdminOverview />
      <h1 className="text-2xl font-semibold text-slate-900">Reports</h1>
      <p className="mt-1 text-slate-600">
        Uphold a report when it is correct (an upheld listing report lowers that listing&apos;s trust score for 90 days), or dismiss
        it. Suspending a listing or account is a separate action on its own page.
      </p>
      <div className="mt-4 flex gap-2">
        {(["OPEN", "UPHELD", "DISMISSED"] as const).map((s) => (
          <button key={s} type="button" aria-pressed={status === s} onClick={() => setStatus(s)}
            className={`rounded-full border px-3 py-1 text-sm ${status === s ? "border-teal-700 bg-teal-700 text-white" : "border-slate-300 bg-white text-slate-700"}`}>
            {s === "OPEN" ? "Open" : s === "UPHELD" ? "Upheld" : "Dismissed"}
          </button>
        ))}
      </div>
      {message && <div className="mt-4"><FormMessage kind={message.kind}>{message.text}</FormMessage></div>}
      {data && data.items.length === 0 && <p className="mt-6 text-slate-600">No reports here.</p>}
      <ul className="mt-4 space-y-3">
        {data?.items.map((r) => (
          <ReportItem key={r.id} report={r} onResolved={(text) => { setMessage({ kind: "success", text }); load(); }} onError={(text) => setMessage({ kind: "error", text })} />
        ))}
      </ul>
    </div>
  );
}

function ReportItem({ report: r, onResolved, onError }: { report: ReportView; onResolved: (t: string) => void; onError: (t: string) => void }) {
  const [note, setNote] = useState("");
  const [hideReason, setHideReason] = useState("");

  async function resolve(event: FormEvent, outcome: "UPHELD" | "DISMISSED") {
    event.preventDefault();
    try {
      await api(`/api/v1/admin/reports/${r.id}/resolve`, { method: "POST", json: { outcome, note } });
      onResolved(outcome === "UPHELD" ? "Report upheld." : "Report dismissed.");
    } catch (e) {
      onError(errorMessage(e));
    }
  }

  async function hideReview() {
    try {
      await api(`/api/v1/admin/reviews/${r.targetId}/hide`, { method: "POST", json: { reason: hideReason } });
      onResolved("Review hidden. Remember to resolve the report too.");
    } catch (e) {
      onError(errorMessage(e));
    }
  }

  const link = r.targetType === "LISTING" ? `/admin/listings/${r.targetId}` : r.targetType === "USER" ? "/admin" : null;

  return (
    <li className="rounded-lg border border-slate-200 bg-white p-4 text-sm">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <span className="font-medium text-slate-900">{REASON_LABELS[r.reason]}</span>
        <span className="text-xs text-slate-500">{formatDateTime(r.createdAt)} · reported by {r.reporterName}</span>
      </div>
      <p className="mt-1 text-slate-700">
        {link ? <Link href={link} className="underline underline-offset-4">{r.targetSummary}</Link> : r.targetSummary}
      </p>
      {r.details && <p className="mt-1 text-slate-600">“{r.details}”</p>}
      {r.status !== "OPEN" && <p className="mt-2 text-slate-600">Resolution: {r.resolutionNote}</p>}

      {r.status === "OPEN" && (
        <form className="mt-3 space-y-2" onSubmit={(e) => resolve(e, "UPHELD")}>
          {r.targetType === "REVIEW" && (
            <div className="flex flex-wrap gap-2">
              <input value={hideReason} onChange={(e) => setHideReason(e.target.value)} placeholder="Reason for hiding the review" className="flex-1 rounded-md border border-slate-300 px-2 py-1" />
              <button type="button" disabled={!hideReason.trim()} onClick={hideReview} className="rounded-md border border-slate-300 px-3 py-1 disabled:opacity-50">Hide review</button>
            </div>
          )}
          <label className="block">
            <span className="text-slate-700">Resolution note (required, kept for the record)</span>
            <input value={note} onChange={(e) => setNote(e.target.value)} maxLength={1000} className="mt-1 block w-full rounded-md border border-slate-300 px-2 py-1" />
          </label>
          <div className="flex gap-2">
            <button type="submit" disabled={!note.trim()} className="rounded-md bg-red-700 px-3 py-1 text-white disabled:opacity-50">Uphold</button>
            <button type="button" disabled={!note.trim()} onClick={(e) => resolve(e, "DISMISSED")} className="rounded-md border border-slate-300 px-3 py-1 disabled:opacity-50">Dismiss</button>
          </div>
        </form>
      )}
    </li>
  );
}
