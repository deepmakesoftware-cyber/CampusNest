"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth";
import { ROLE_LABELS } from "@/lib/types";

const navLink = "text-slate-700 hover:text-slate-900";

export default function SiteHeader() {
  const { state, logout } = useAuth();
  const router = useRouter();

  async function handleLogout() {
    await logout();
    router.push("/");
  }

  const role = state.status === "authenticated" ? state.user.role : null;

  return (
    <header className="border-b border-slate-200 bg-white">
      <div className="mx-auto flex w-full max-w-6xl flex-wrap items-center justify-between gap-3 px-4 py-3">
        <Link href="/" className="text-lg font-semibold text-teal-800">
          CampusNest
        </Link>

        <nav aria-label="Main" className="flex flex-wrap items-center gap-x-4 gap-y-2 text-sm">
          {role !== "OWNER" && role !== "ADMIN" && <Link href="/listings" className={navLink}>Find accommodation</Link>}

          {role === "STUDENT" && (
            <>
              <Link href="/student/recommendations" className={navLink}>Recommended</Link>
              <Link href="/student/roommates" className={navLink}>Roommates</Link>
              <Link href="/student" className={navLink}>Dashboard</Link>
              <Link href="/student/saved" className={navLink}>Saved</Link>
              <Link href="/messages" className={navLink}>Messages</Link>
            </>
          )}
          {role === "OWNER" && (
            <>
              <Link href="/owner" className={navLink}>My listings</Link>
              <Link href="/messages" className={navLink}>Messages</Link>
            </>
          )}
          {role === "ADMIN" && (
            <>
              <Link href="/admin/listings" className={navLink}>Listing review</Link>
              <Link href="/admin" className={navLink}>Users</Link>
              <Link href="/admin/fares" className={navLink}>Fares</Link>
            </>
          )}

          {state.status === "anonymous" && (
            <>
              <Link href="/login" className={navLink}>Log in</Link>
              <Link href="/register" className="rounded-md bg-teal-700 px-3 py-1.5 font-medium text-white hover:bg-teal-800">Sign up</Link>
            </>
          )}

          {state.status === "authenticated" && (
            <>
              <Link href="/account" className={navLink}>Account</Link>
              <span className="text-slate-500">
                {state.user.displayName}
                <span className="ml-2 rounded bg-slate-100 px-1.5 py-0.5 text-xs text-slate-600">{ROLE_LABELS[state.user.role]}</span>
              </span>
              <button type="button" onClick={handleLogout} className="text-slate-700 underline-offset-4 hover:underline">
                Log out
              </button>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}
