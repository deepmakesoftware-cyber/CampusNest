"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { AdminOverview as Overview } from "@/lib/types";

/** "What needs attention" strip shown on admin pages. */
export default function AdminOverview() {
  const [data, setData] = useState<Overview | null>(null);
  useEffect(() => {
    api<Overview>("/api/v1/admin/overview").then(setData).catch(() => setData(null));
  }, []);
  if (!data) return null;
  const items = [
    { href: "/admin/listings", label: "Listings awaiting review", value: data.listingsAwaitingReview },
    { href: "/admin/reports", label: "Open reports", value: data.openReports },
    { href: "/admin/owners", label: "Owner verification requests", value: data.ownerVerificationRequests },
  ];
  return (
    <div className="mb-6 grid gap-3 sm:grid-cols-3">
      {items.map((i) => (
        <Link key={i.href} href={i.href} className="rounded-lg border border-slate-200 bg-white p-3 hover:border-teal-600">
          <div className={`text-2xl font-semibold ${i.value > 0 ? "text-slate-900" : "text-slate-400"}`}>{i.value}</div>
          <div className="text-sm text-slate-600">{i.label}</div>
        </Link>
      ))}
    </div>
  );
}
