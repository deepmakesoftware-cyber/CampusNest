"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";
import ListingDetailsView, { StatusHistory } from "@/components/ListingDetailsView";
import ListingStatusBadge from "@/components/ListingStatusBadge";
import { FormMessage } from "@/components/form";
import { api, errorMessage } from "@/lib/api";
import { formatDate } from "@/lib/listingLabels";
import type { AdminListingDetail } from "@/lib/types";

type ReasonAction = "request-changes" | "reject" | "suspend";

const REASON_ACTIONS: Record<ReasonAction, { label: string; prompt: string; button: string; danger: boolean }> = {
  "request-changes": {
    label: "Request changes",
    prompt: "What should the owner change? They will see this message.",
    button: "Send request",
    danger: false,
  },
  reject: {
    label: "Reject",
    prompt: "Why is this listing rejected? Rejection is final; the owner will see this message.",
    button: "Reject listing",
    danger: true,
  },
  suspend: {
    label: "Suspend",
    prompt: "Why is this listing being hidden from students? The owner will see this message.",
    button: "Suspend listing",
    danger: true,
  },
};

const OWNER_TYPE = { INDIVIDUAL: "Individual owner", PG_OPERATOR: "PG operator", HOSTEL_OPERATOR: "Hostel operator" } as const;
const VERIFICATION = { UNVERIFIED: "Not verified", PENDING: "Verification pending", VERIFIED: "Identity checked", REJECTED: "Verification rejected" } as const;

export default function AdminListingReview() {
  const { id } = useParams<{ id: string }>();
  const [data, setData] = useState<AdminListingDetail | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [message, setMessage] = useState<{ kind: "error" | "success"; text: string } | null>(null);
  const [reasonAction, setReasonAction] = useState<ReasonAction | null>(null);
  const [reason, setReason] = useState("");
  const [pending, setPending] = useState(false);

  useEffect(() => {
    api<AdminListingDetail>(`/api/v1/admin/listings/${id}`)
      .then(setData)
      .catch((e) => setLoadError(errorMessage(e)));
  }, [id]);

  async function act(action: "approve" | "reinstate" | ReasonAction, successText: string) {
    setPending(true);
    setMessage(null);
    try {
      const updated = await api<AdminListingDetail>(`/api/v1/admin/listings/${id}/${action}`, {
        method: "POST",
        json: reasonAction ? { reason } : undefined,
      });
      setData(updated);
      setReasonAction(null);
      setReason("");
      setMessage({ kind: "success", text: successText });
    } catch (e) {
      setMessage({ kind: "error", text: errorMessage(e) });
    } finally {
      setPending(false);
    }
  }

  function submitReason(event: FormEvent) {
    event.preventDefault();
    if (reasonAction) act(reasonAction, `${REASON_ACTIONS[reasonAction].label}: done. The owner can see your message.`);
  }

  if (loadError) return <FormMessage kind="error">{loadError}</FormMessage>;
  if (!data) return <p className="text-slate-600">Loading…</p>;

  const { listing, owner } = data;
  const status = listing.status;

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_20rem]">
      <div className="space-y-4">
        <Link href="/admin/listings" className="text-sm text-teal-700 underline underline-offset-4">
          ← Listing review
        </Link>
        <div className="flex flex-wrap items-center gap-3">
          <ListingStatusBadge status={status} />
          <span className="text-sm text-slate-500">Submitted {formatDate(listing.submittedAt)}</span>
        </div>
        {listing.statusReason && (
          <p className="rounded-md border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-700">
            Latest message to owner: “{listing.statusReason}”
          </p>
        )}
        <div className="rounded-lg border border-slate-200 bg-white p-5 sm:p-6">
          <ListingDetailsView listing={listing} />
        </div>
      </div>

      <aside className="space-y-4">
        <section className="rounded-lg border border-slate-200 bg-white p-5">
          <h2 className="font-semibold text-slate-900">Decision</h2>
          {message && <div className="mt-3"><FormMessage kind={message.kind}>{message.text}</FormMessage></div>}

          {status === "PENDING_VERIFICATION" && (
            <div className="mt-3 space-y-2">
              <p className="text-sm text-slate-600">
                Check the photos, address, location pin and charges look genuine and consistent before approving.
              </p>
              <button type="button" disabled={pending} onClick={() => act("approve", "Approved. Students can now see this listing.")} className="w-full rounded-md bg-teal-700 px-3 py-2 font-medium text-white hover:bg-teal-800 disabled:opacity-60">
                Approve and publish
              </button>
              <ReasonButton action="request-changes" current={reasonAction} onSelect={setReasonAction} />
              <ReasonButton action="reject" current={reasonAction} onSelect={setReasonAction} />
            </div>
          )}
          {status.startsWith("PUBLISHED") && (
            <div className="mt-3 space-y-2">
              <p className="text-sm text-slate-600">This listing is visible to students.</p>
              <ReasonButton action="suspend" current={reasonAction} onSelect={setReasonAction} />
            </div>
          )}
          {status === "SUSPENDED" && (
            <div className="mt-3 space-y-2">
              <p className="text-sm text-slate-600">Reinstated listings return as “full” until the owner re-confirms availability.</p>
              <button type="button" disabled={pending} onClick={() => act("reinstate", "Reinstated.")} className="w-full rounded-md border border-teal-700 px-3 py-2 font-medium text-teal-800 hover:bg-teal-50 disabled:opacity-60">
                Reinstate
              </button>
            </div>
          )}
          {(status === "CHANGES_REQUESTED" || status === "REJECTED") && (
            <p className="mt-3 text-sm text-slate-600">
              {status === "REJECTED" ? "This listing was rejected. No further actions." : "Waiting for the owner to update and resubmit."}
            </p>
          )}

          {reasonAction && (
            <form onSubmit={submitReason} className="mt-4 space-y-2">
              <label htmlFor="reason" className="block text-sm text-slate-800">{REASON_ACTIONS[reasonAction].prompt}</label>
              <textarea id="reason" value={reason} onChange={(e) => setReason(e.target.value)} rows={4} maxLength={1000} className="block w-full rounded-md border border-slate-300 px-3 py-2 text-sm" />
              <div className="flex gap-2">
                <button type="submit" disabled={pending || !reason.trim()} className={`rounded-md px-3 py-1.5 text-sm font-medium text-white disabled:opacity-50 ${REASON_ACTIONS[reasonAction].danger ? "bg-red-700" : "bg-teal-700"}`}>
                  {REASON_ACTIONS[reasonAction].button}
                </button>
                <button type="button" onClick={() => setReasonAction(null)} className="rounded-md border border-slate-300 px-3 py-1.5 text-sm">
                  Cancel
                </button>
              </div>
            </form>
          )}
        </section>

        <section className="rounded-lg border border-slate-200 bg-white p-5 text-sm">
          <h2 className="font-semibold text-slate-900">Owner</h2>
          <dl className="mt-2 space-y-1 text-slate-700">
            <div><dt className="inline text-slate-500">Name: </dt><dd className="inline">{owner.displayName}</dd></div>
            <div><dt className="inline text-slate-500">Email: </dt><dd className="inline break-all">{owner.email}</dd></div>
            <div><dt className="inline text-slate-500">Phone: </dt><dd className="inline">{owner.phone ?? "Not provided"}</dd></div>
            <div><dt className="inline text-slate-500">Type: </dt><dd className="inline">{owner.ownerType ? OWNER_TYPE[owner.ownerType] : "—"}{owner.businessName && ` · ${owner.businessName}`}</dd></div>
            <div><dt className="inline text-slate-500">Identity: </dt><dd className="inline">{VERIFICATION[owner.verificationStatus]}</dd></div>
          </dl>
          <p className="mt-2 text-xs text-slate-500">Approving a listing does not verify the owner&apos;s identity; that is a separate check.</p>
        </section>

        <section className="rounded-lg border border-slate-200 bg-white p-5">
          <h2 className="font-semibold text-slate-900">History</h2>
          <div className="mt-3">
            <StatusHistory history={listing.history} />
          </div>
        </section>
      </aside>
    </div>
  );
}

function ReasonButton({ action, current, onSelect }: { action: ReasonAction; current: ReasonAction | null; onSelect: (a: ReasonAction) => void }) {
  const config = REASON_ACTIONS[action];
  return (
    <button
      type="button"
      onClick={() => onSelect(action)}
      aria-pressed={current === action}
      className={`w-full rounded-md border px-3 py-2 text-sm font-medium ${
        config.danger ? "border-red-300 text-red-800 hover:bg-red-50" : "border-slate-300 text-slate-800 hover:bg-slate-50"
      }`}
    >
      {config.label}…
    </button>
  );
}
