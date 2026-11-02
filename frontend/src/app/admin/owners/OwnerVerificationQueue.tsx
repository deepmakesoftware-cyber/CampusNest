"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import AdminOverview from "@/components/AdminOverview";
import { FormMessage } from "@/components/form";
import { api, errorMessage } from "@/lib/api";
import { formatDate, formatDateTime } from "@/lib/listingLabels";
import type { OwnerVerificationView, VerificationMethod, VerificationStatus } from "@/lib/types";

const METHOD_LABELS: Record<VerificationMethod, string> = {
  PHONE_CALL: "Phone call",
  VIDEO_CALL: "Video call",
  IN_PERSON_VISIT: "In-person visit",
  DOCUMENT_SEEN_NOT_STORED: "Saw an ID document (not stored)",
  OTHER: "Other",
};

const FILTERS: { value: VerificationStatus | ""; label: string }[] = [
  { value: "PENDING", label: "Requests" },
  { value: "VERIFIED", label: "Verified" },
  { value: "REJECTED", label: "Rejected" },
  { value: "", label: "All owners" },
];

export default function OwnerVerificationQueue() {
  const [filter, setFilter] = useState<VerificationStatus | "">("PENDING");
  const [owners, setOwners] = useState<OwnerVerificationView[] | null>(null);
  const [message, setMessage] = useState<{ kind: "error" | "success"; text: string } | null>(null);

  const load = useCallback(() => {
    api<OwnerVerificationView[]>(`/api/v1/admin/owners${filter ? `?status=${filter}` : ""}`)
      .then(setOwners).catch((e) => setMessage({ kind: "error", text: errorMessage(e) }));
  }, [filter]);
  useEffect(load, [load]);

  return (
    <div>
      <AdminOverview />
      <h1 className="text-2xl font-semibold text-slate-900">Owner identity verification</h1>
      <p className="mt-1 text-slate-600">
        Check that the owner is who they say they are (for example, call the phone number and confirm the property address).
        Record how you checked. Do not store ID documents. “Verified” means identity checked, not that a property is safe.
      </p>
      <div className="mt-4 flex flex-wrap gap-2">
        {FILTERS.map((f) => (
          <button key={f.label} type="button" aria-pressed={filter === f.value} onClick={() => setFilter(f.value)}
            className={`rounded-full border px-3 py-1 text-sm ${filter === f.value ? "border-teal-700 bg-teal-700 text-white" : "border-slate-300 bg-white text-slate-700"}`}>
            {f.label}
          </button>
        ))}
      </div>
      {message && <div className="mt-4"><FormMessage kind={message.kind}>{message.text}</FormMessage></div>}
      {owners && owners.length === 0 && <p className="mt-6 text-slate-600">No owners here.</p>}
      <ul className="mt-4 space-y-3">
        {owners?.map((o) => (
          <OwnerItem key={o.ownerId} owner={o} onDone={(text) => { setMessage({ kind: "success", text }); load(); }} onError={(text) => setMessage({ kind: "error", text })} />
        ))}
      </ul>
    </div>
  );
}

function OwnerItem({ owner: o, onDone, onError }: { owner: OwnerVerificationView; onDone: (t: string) => void; onError: (t: string) => void }) {
  const [method, setMethod] = useState<VerificationMethod>("PHONE_CALL");
  const [notes, setNotes] = useState("");

  async function decide(event: FormEvent | null, decision: "VERIFIED" | "REJECTED" | "REVOKED") {
    event?.preventDefault();
    try {
      await api(`/api/v1/admin/owners/${o.ownerId}/verification`, { method: "POST", json: { decision, method, notes } });
      onDone(`${o.displayName}: ${decision.toLowerCase()}.`);
    } catch (e) {
      onError(errorMessage(e));
    }
  }

  return (
    <li className="rounded-lg border border-slate-200 bg-white p-4 text-sm">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <span className="font-medium text-slate-900">{o.displayName}{o.businessName && ` · ${o.businessName}`}</span>
        <span className="text-xs text-slate-500">{o.status}{o.requestedAt && ` · requested ${formatDate(o.requestedAt)}`}</span>
      </div>
      <p className="mt-1 text-slate-600">{o.email}{o.phone ? ` · ${o.phone}` : " · no phone given"} · {o.listingCount} listing{o.listingCount === 1 ? "" : "s"}</p>

      {o.history.length > 0 && (
        <ul className="mt-2 space-y-1 text-xs text-slate-500">
          {o.history.map((h, i) => (
            <li key={i}>{formatDateTime(h.at)}: {h.decision.toLowerCase()} by {h.reviewer} via {METHOD_LABELS[h.method].toLowerCase()}: “{h.notes}”</li>
          ))}
        </ul>
      )}

      {o.status !== "REJECTED" && (
        <form className="mt-3 space-y-2" onSubmit={(e) => decide(e, o.status === "VERIFIED" ? "REVOKED" : "VERIFIED")}>
          <div className="grid gap-2 sm:grid-cols-[14rem_1fr]">
            <select value={method} onChange={(e) => setMethod(e.target.value as VerificationMethod)} className="rounded-md border border-slate-300 bg-white px-2 py-1">
              {(Object.keys(METHOD_LABELS) as VerificationMethod[]).map((m) => <option key={m} value={m}>{METHOD_LABELS[m]}</option>)}
            </select>
            <input value={notes} onChange={(e) => setNotes(e.target.value)} maxLength={1000} placeholder="What did you check? (required)" className="rounded-md border border-slate-300 px-2 py-1" />
          </div>
          <div className="flex gap-2">
            {o.status === "VERIFIED" ? (
              <button type="submit" disabled={!notes.trim()} className="rounded-md bg-red-700 px-3 py-1 text-white disabled:opacity-50">Revoke verification</button>
            ) : (
              <>
                <button type="submit" disabled={!notes.trim()} className="rounded-md bg-teal-700 px-3 py-1 text-white disabled:opacity-50">Mark identity checked</button>
                {o.status === "PENDING" && (
                  <button type="button" disabled={!notes.trim()} onClick={() => decide(null, "REJECTED")} className="rounded-md border border-slate-300 px-3 py-1 disabled:opacity-50">Reject</button>
                )}
              </>
            )}
          </div>
        </form>
      )}
    </li>
  );
}
