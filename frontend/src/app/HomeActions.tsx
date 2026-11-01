"use client";

import Link from "next/link";
import { ROLE_HOME, useAuth } from "@/lib/auth";

export default function HomeActions() {
  const { state } = useAuth();

  if (state.status === "authenticated") {
    return (
      <Link href={ROLE_HOME[state.user.role]} className="rounded-md bg-teal-700 px-4 py-2 font-medium text-white hover:bg-teal-800">
        Go to your dashboard
      </Link>
    );
  }
  return (
    <div className="flex flex-wrap gap-3">
      <Link href="/listings" className="rounded-md bg-teal-700 px-4 py-2 font-medium text-white hover:bg-teal-800">
        Browse accommodation
      </Link>
      <Link href="/register" className="rounded-md border border-slate-300 bg-white px-4 py-2 font-medium text-slate-800 hover:bg-slate-50">
        Create a free account
      </Link>
    </div>
  );
}
