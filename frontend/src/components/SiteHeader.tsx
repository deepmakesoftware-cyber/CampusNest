"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { ROLE_HOME, useAuth } from "@/lib/auth";
import { ROLE_LABELS } from "@/lib/types";

export default function SiteHeader() {
  const { state, logout } = useAuth();
  const router = useRouter();

  async function handleLogout() {
    await logout();
    router.push("/");
  }

  return (
    <header className="border-b border-slate-200 bg-white">
      <div className="mx-auto flex w-full max-w-5xl flex-wrap items-center justify-between gap-3 px-4 py-3">
        <Link href="/" className="text-lg font-semibold text-teal-800">
          CampusNest
        </Link>

        {state.status === "anonymous" && (
          <nav className="flex items-center gap-4 text-sm">
            <Link href="/login" className="text-slate-700 hover:text-slate-900">
              Log in
            </Link>
            <Link href="/register" className="rounded-md bg-teal-700 px-3 py-1.5 font-medium text-white hover:bg-teal-800">
              Sign up
            </Link>
          </nav>
        )}

        {state.status === "authenticated" && (
          <nav className="flex flex-wrap items-center gap-x-4 gap-y-2 text-sm">
            <Link href={ROLE_HOME[state.user.role]} className="text-slate-700 hover:text-slate-900">
              {state.user.role === "ADMIN" ? "Users" : "Dashboard"}
            </Link>
            <Link href="/account" className="text-slate-700 hover:text-slate-900">
              Account
            </Link>
            <span className="text-slate-500">
              {state.user.displayName}
              <span className="ml-2 rounded bg-slate-100 px-1.5 py-0.5 text-xs text-slate-600">
                {ROLE_LABELS[state.user.role]}
              </span>
            </span>
            <button type="button" onClick={handleLogout} className="text-slate-700 underline-offset-4 hover:underline">
              Log out
            </button>
          </nav>
        )}
      </div>
    </header>
  );
}
