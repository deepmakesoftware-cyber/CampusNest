"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import ComingSoon from "@/components/ComingSoon";
import { FormMessage } from "@/components/form";
import { api, errorMessage } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import type { OwnerProfile } from "@/lib/types";

const VERIFICATION_TEXT: Record<OwnerProfile["verificationStatus"], string> = {
  UNVERIFIED: "Your identity has not been verified yet.",
  PENDING: "Your verification request is being reviewed.",
  VERIFIED: "Your identity has been checked by CampusNest.",
  REJECTED: "Your verification request was not approved.",
};

export default function OwnerHome() {
  const { state } = useAuth();
  const [profile, setProfile] = useState<OwnerProfile | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api<OwnerProfile>("/api/v1/owner/profile")
      .then(setProfile)
      .catch((e) => setError(errorMessage(e)));
  }, []);

  const name = state.status === "authenticated" ? state.user.displayName : "";

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">Hi, {name}</h1>
        <p className="mt-1 text-slate-600">Manage your accommodation listings on CampusNest.</p>
      </div>

      {error && <FormMessage kind="error">{error}</FormMessage>}

      {profile && (
        <section className="rounded-lg border border-slate-200 bg-white p-5 sm:p-6">
          <h2 className="text-lg font-semibold text-slate-900">Provider profile</h2>
          <p className="mt-2 text-slate-700">{VERIFICATION_TEXT[profile.verificationStatus]}</p>
          <p className="mt-1 text-sm text-slate-600">
            {profile.businessName ?? "No business name added."}{" "}
            <Link href="/account" className="text-teal-700 underline underline-offset-4">
              Edit profile
            </Link>
          </p>
        </section>
      )}

      <ComingSoon
        items={[
          { phase: "Phase 3", title: "Create and submit listings", description: "rooms, rent, amenities, photos and location" },
          { phase: "Phase 3", title: "Listing review status", description: "see admin feedback and publish status" },
          { phase: "Phase 4", title: "Student enquiries", description: "reply to students interested in your listing" },
        ]}
      />
    </div>
  );
}
