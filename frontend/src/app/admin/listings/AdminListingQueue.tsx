"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import AdminOverview from "@/components/AdminOverview";
import ListingStatusBadge from "@/components/ListingStatusBadge";
import { FormMessage } from "@/components/form";
import { api, errorMessage } from "@/lib/api";
import { formatDate, formatRupees } from "@/lib/listingLabels";
import type { AdminListingSummary, ListingStatus, Page } from "@/lib/types";

const TABS: { value: ListingStatus | ""; label: string }[] = [
  { value: "PENDING_VERIFICATION", label: "Waiting for review" },
  { value: "CHANGES_REQUESTED", label: "Changes requested" },
  { value: "PUBLISHED_AVAILABLE", label: "Published" },
  { value: "PUBLISHED_UNAVAILABLE", label: "Published · full" },
  { value: "SUSPENDED", label: "Suspended" },
  { value: "REJECTED", label: "Rejected" },
  { value: "", label: "All" },
];

const VERIFICATION_SHORT = { UNVERIFIED: "Not verified", PENDING: "Pending", VERIFIED: "Verified", REJECTED: "Rejected" } as const;

export default function AdminListingQueue() {
  const [tab, setTab] = useState<ListingStatus | "">("PENDING_VERIFICATION");
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<AdminListingSummary> | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const params = new URLSearchParams({ page: String(page) });
    if (tab) params.set("status", tab);
    api<Page<AdminListingSummary>>(`/api/v1/admin/listings?${params}`)
      .then((result) => {
        setData(result);
        setError(null);
      })
      .catch((e) => setError(errorMessage(e)));
  }, [tab, page]);

  return (
    <div>
      <AdminOverview />
      <h1 className="text-2xl font-semibold text-slate-900">Listing review</h1>
      <p className="mt-1 text-slate-600">Check submitted listings before students can see them. Oldest submissions are shown first.</p>

      <div className="mt-6 flex flex-wrap gap-2" role="group" aria-label="Filter by status">
        {TABS.map((t) => (
          <button
            key={t.label}
            type="button"
            onClick={() => {
              setTab(t.value);
              setPage(0);
            }}
            aria-pressed={tab === t.value}
            className={`rounded-full border px-3 py-1 text-sm ${
              tab === t.value ? "border-teal-700 bg-teal-700 text-white" : "border-slate-300 bg-white text-slate-700"
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {error && <div className="mt-4"><FormMessage kind="error">{error}</FormMessage></div>}

      {data && data.items.length === 0 && (
        <p className="mt-8 text-slate-600">{tab === "PENDING_VERIFICATION" ? "Nothing waiting for review. 🎉" : "No listings in this group."}</p>
      )}

      {data && data.items.length > 0 && (
        <ul className="mt-4 divide-y divide-slate-100 rounded-lg border border-slate-200 bg-white">
          {data.items.map((l) => (
            <li key={l.id}>
              <Link href={`/admin/listings/${l.id}`} className="flex flex-col gap-1 px-4 py-3 hover:bg-slate-50 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <div className="font-medium text-slate-900">{l.title}</div>
                  <div className="mt-0.5 text-sm text-slate-600">
                    {l.locality ?? "No locality"} · by {l.ownerName} ({VERIFICATION_SHORT[l.ownerVerification]})
                    {l.minimumRent !== null && ` · from ${formatRupees(l.minimumRent)}`}
                  </div>
                </div>
                <div className="flex items-center gap-3 text-sm text-slate-500">
                  <span>Submitted {formatDate(l.submittedAt)}</span>
                  <ListingStatusBadge status={l.status} />
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}

      {data && data.totalPages > 1 && (
        <div className="mt-4 flex items-center gap-3 text-sm">
          <button type="button" disabled={page === 0} onClick={() => setPage(page - 1)} className="rounded border border-slate-300 px-3 py-1 disabled:opacity-50">
            Previous
          </button>
          <span className="text-slate-600">Page {data.page + 1} of {data.totalPages}</span>
          <button type="button" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)} className="rounded border border-slate-300 px-3 py-1 disabled:opacity-50">
            Next
          </button>
        </div>
      )}
    </div>
  );
}
