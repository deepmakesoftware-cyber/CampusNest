"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { FormMessage } from "@/components/form";
import { api, errorMessage } from "@/lib/api";
import { formatDate } from "@/lib/listingLabels";
import type { SavedListingView } from "@/lib/types";

export default function SavedListings() {
  const [items, setItems] = useState<SavedListingView[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api<SavedListingView[]>("/api/v1/student/saved")
      .then(setItems)
      .catch((e) => setError(errorMessage(e)));
  }, []);

  async function remove(listingId: number) {
    try {
      await api<void>(`/api/v1/student/saved/${listingId}`, { method: "DELETE" });
      setItems((list) => list?.filter((i) => i.listingId !== listingId) ?? null);
    } catch (e) {
      setError(errorMessage(e));
    }
  }

  return (
    <div>
      <h1 className="text-2xl font-semibold text-slate-900">Saved listings</h1>
      <p className="mt-1 text-slate-600">Places you bookmarked to compare later.</p>
      {error && <div className="mt-4"><FormMessage kind="error">{error}</FormMessage></div>}
      {items && items.length === 0 && (
        <div className="mt-6 rounded-lg border border-dashed border-slate-300 bg-white p-8 text-center">
          <p className="font-medium text-slate-900">Nothing saved yet.</p>
          <Link href="/listings" className="mt-2 inline-block text-teal-700 underline underline-offset-4">Browse accommodation</Link>
        </div>
      )}
      {items && items.length > 0 && (
        <ul className="mt-6 divide-y divide-slate-100 rounded-lg border border-slate-200 bg-white">
          {items.map((s) => (
            <li key={s.listingId} className="flex items-center justify-between gap-3 px-4 py-3">
              <div>
                {s.available ? (
                  <Link href={`/listings/${s.listingId}`} className="font-medium text-slate-900 hover:underline">{s.title}</Link>
                ) : (
                  <span className="font-medium text-slate-500">{s.title}</span>
                )}
                <div className="text-sm text-slate-600">
                  {s.locality} · saved {formatDate(s.savedAt)}
                  {!s.available && <span className="ml-1 text-red-700">· no longer listed</span>}
                </div>
              </div>
              <button type="button" onClick={() => remove(s.listingId)} className="text-sm text-red-700 hover:underline">Remove</button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
