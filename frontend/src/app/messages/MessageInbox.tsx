"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { FormMessage } from "@/components/form";
import { api, errorMessage } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { formatDateTime } from "@/lib/listingLabels";
import type { EnquirySummary } from "@/lib/types";

export default function MessageInbox() {
  const { state } = useAuth();
  const [items, setItems] = useState<EnquirySummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const role = state.status === "authenticated" ? state.user.role : null;

  useEffect(() => {
    if (!role) return;
    api<EnquirySummary[]>(role === "OWNER" ? "/api/v1/owner/enquiries" : "/api/v1/student/enquiries")
      .then(setItems)
      .catch((e) => setError(errorMessage(e)));
  }, [role]);

  return (
    <div>
      <h1 className="text-2xl font-semibold text-slate-900">Messages</h1>
      <p className="mt-1 text-slate-600">
        {role === "OWNER" ? "Questions from students about your listings." : "Your conversations with owners."} Only you and the other person can read them.
      </p>
      {error && <div className="mt-4"><FormMessage kind="error">{error}</FormMessage></div>}
      {items && items.length === 0 && (
        <div className="mt-6 rounded-lg border border-dashed border-slate-300 bg-white p-8 text-center text-slate-600">
          {role === "OWNER" ? "No messages yet. Students can contact you once your listing is published." : (
            <>No conversations yet. <Link href="/listings" className="text-teal-700 underline underline-offset-4">Find a place</Link> and message the owner.</>
          )}
        </div>
      )}
      {items && items.length > 0 && (
        <ul className="mt-6 divide-y divide-slate-100 rounded-lg border border-slate-200 bg-white">
          {items.map((e) => (
            <li key={e.id}>
              <Link href={`/messages/${e.id}`} className="block px-4 py-3 hover:bg-slate-50">
                <div className="flex items-center justify-between gap-3">
                  <span className="font-medium text-slate-900">{e.otherName}</span>
                  <span className="text-xs text-slate-500">{formatDateTime(e.lastMessageAt)}</span>
                </div>
                <div className="text-sm text-slate-600">{e.listingTitle}</div>
                <div className="mt-0.5 flex items-center gap-2 text-sm text-slate-700">
                  {e.waitingForMyReply && <span className="rounded bg-amber-50 px-1.5 py-0.5 text-xs font-medium text-amber-800">Awaiting your reply</span>}
                  <span className="truncate">{e.lastMessage}</span>
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
