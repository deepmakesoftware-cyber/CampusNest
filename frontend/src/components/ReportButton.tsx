"use client";

import { useState, type FormEvent } from "react";
import { api, errorMessage } from "@/lib/api";
import type { ReportReason, ReportTarget } from "@/lib/types";

const REASONS: Record<ReportTarget, { value: ReportReason; label: string }[]> = {
  LISTING: [
    { value: "FAKE_LISTING", label: "Looks fake or doesn't exist" },
    { value: "WRONG_INFORMATION", label: "Wrong rent, rooms or details" },
    { value: "UNSAFE", label: "Safety concern" },
    { value: "OTHER", label: "Something else" },
  ],
  REVIEW: [
    { value: "INAPPROPRIATE", label: "Abusive or inappropriate" },
    { value: "SPAM", label: "Spam or advertising" },
    { value: "WRONG_INFORMATION", label: "Misleading" },
    { value: "OTHER", label: "Something else" },
  ],
  USER: [
    { value: "HARASSMENT", label: "Harassment" },
    { value: "SPAM", label: "Spam or scam" },
    { value: "INAPPROPRIATE", label: "Inappropriate behaviour" },
    { value: "OTHER", label: "Something else" },
  ],
};

/** A small "Report" link that opens an inline form. Reports go to CampusNest administrators, not to the other person. */
export default function ReportButton({ targetType, targetId, label = "Report" }: { targetType: ReportTarget; targetId: number; label?: string }) {
  const [open, setOpen] = useState(false);
  const [reason, setReason] = useState<ReportReason>(REASONS[targetType][0].value);
  const [details, setDetails] = useState("");
  const [state, setState] = useState<{ kind: "idle" | "sending" | "done" | "error"; text?: string }>({ kind: "idle" });

  async function send(event: FormEvent) {
    event.preventDefault();
    setState({ kind: "sending" });
    try {
      await api("/api/v1/reports", { method: "POST", json: { targetType, targetId, reason, details } });
      setState({ kind: "done", text: "Thank you. An administrator will look at this." });
    } catch (e) {
      setState({ kind: "error", text: errorMessage(e) });
    }
  }

  if (state.kind === "done") return <span className="text-xs text-green-800">{state.text}</span>;
  if (!open) {
    return (
      <button type="button" onClick={() => setOpen(true)} className="text-xs text-slate-500 underline underline-offset-4 hover:text-red-700">
        {label}
      </button>
    );
  }
  return (
    <form onSubmit={send} className="mt-2 space-y-2 rounded-md border border-slate-200 bg-slate-50 p-3 text-sm">
      <label className="block">
        <span className="text-slate-700">What is wrong?</span>
        <select value={reason} onChange={(e) => setReason(e.target.value as ReportReason)} className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-2 py-1">
          {REASONS[targetType].map((r) => <option key={r.value} value={r.value}>{r.label}</option>)}
        </select>
      </label>
      <label className="block">
        <span className="text-slate-700">Details (optional)</span>
        <textarea value={details} onChange={(e) => setDetails(e.target.value)} maxLength={1000} rows={2} className="mt-1 block w-full rounded-md border border-slate-300 px-2 py-1" />
      </label>
      {state.kind === "error" && <p className="text-red-700">{state.text}</p>}
      <div className="flex gap-2">
        <button type="submit" disabled={state.kind === "sending"} className="rounded-md bg-red-700 px-3 py-1 text-white disabled:opacity-50">Send report</button>
        <button type="button" onClick={() => setOpen(false)} className="rounded-md border border-slate-300 px-3 py-1">Cancel</button>
      </div>
    </form>
  );
}
