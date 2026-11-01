"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, type ReactNode } from "react";
import { ROLE_HOME, useAuth } from "@/lib/auth";
import { ROLE_LABELS, type Role } from "@/lib/types";

/**
 * Shows children only to logged-in users with one of the allowed roles.
 * This is a convenience for the user interface ONLY. The real protection is on the backend:
 * even if someone bypasses this component, the API refuses their requests (401/403).
 */
export default function RoleGate({ allow, children }: { allow: Role[]; children: ReactNode }) {
  const { state } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (state.status === "anonymous") {
      router.replace("/login");
    }
  }, [state.status, router]);

  if (state.status !== "authenticated") {
    return <p className="px-4 py-12 text-center text-slate-600">Loading…</p>;
  }

  const { role } = state.user;
  if (!allow.includes(role)) {
    return (
      <div className="mx-auto max-w-md px-4 py-16 text-center">
        <h1 className="text-xl font-semibold text-slate-900">This page isn&apos;t available to you</h1>
        <p className="mt-2 text-slate-600">
          It is only for {allow.map((r) => ROLE_LABELS[r].toLowerCase()).join(" or ")} accounts.
        </p>
        <Link href={ROLE_HOME[role]} className="mt-6 inline-block text-teal-700 underline underline-offset-4">
          Go to your dashboard
        </Link>
      </div>
    );
  }
  return <>{children}</>;
}
