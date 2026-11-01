"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";
import ComingSoon from "@/components/ComingSoon";
import ListingStatusBadge from "@/components/ListingStatusBadge";
import { FormMessage, SubmitButton, TextField } from "@/components/form";
import { ApiError, api, errorMessage } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { formatDate, formatRupees } from "@/lib/listingLabels";
import type { ListingDetail, ListingSummary, OwnerProfile } from "@/lib/types";

const VERIFICATION_TEXT: Record<OwnerProfile["verificationStatus"], string> = {
  UNVERIFIED: "Your identity has not been verified yet.",
  PENDING: "Your verification request is being reviewed.",
  VERIFIED: "Your identity has been checked by CampusNest.",
  REJECTED: "Your verification request was not approved.",
};

export default function OwnerHome() {
  const { state } = useAuth();
  const router = useRouter();
  const [profile, setProfile] = useState<OwnerProfile | null>(null);
  const [listings, setListings] = useState<ListingSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [title, setTitle] = useState("");
  const [titleError, setTitleError] = useState<string | undefined>();
  const [creating, setCreating] = useState(false);

  useEffect(() => {
    Promise.all([api<OwnerProfile>("/api/v1/owner/profile"), api<ListingSummary[]>("/api/v1/owner/listings")])
      .then(([p, l]) => {
        setProfile(p);
        setListings(l);
      })
      .catch((e) => setError(errorMessage(e)));
  }, []);

  async function createListing(event: FormEvent) {
    event.preventDefault();
    setCreating(true);
    setTitleError(undefined);
    try {
      const created = await api<ListingDetail>("/api/v1/owner/listings", { method: "POST", json: { title } });
      router.push(`/owner/listings/${created.id}`);
    } catch (e) {
      setTitleError(e instanceof ApiError && e.fields.title ? `Title ${e.fields.title}` : errorMessage(e));
      setCreating(false);
    }
  }

  const name = state.status === "authenticated" ? state.user.displayName : "";

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">Hi, {name}</h1>
        <p className="mt-1 text-slate-600">Manage your accommodation listings on CampusNest.</p>
      </div>

      {error && <FormMessage kind="error">{error}</FormMessage>}

      <section className="rounded-lg border border-slate-200 bg-white p-5 sm:p-6">
        <h2 className="text-lg font-semibold text-slate-900">Your listings</h2>
        {listings === null && !error && <p className="mt-3 text-slate-600">Loading…</p>}
        {listings && listings.length === 0 && (
          <p className="mt-3 text-slate-600">You haven&apos;t created any listings yet. Start with a title below.</p>
        )}
        {listings && listings.length > 0 && (
          <ul className="mt-4 divide-y divide-slate-100">
            {listings.map((l) => (
              <li key={l.id} className="flex flex-col gap-1 py-3 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <Link href={`/owner/listings/${l.id}`} className="font-medium text-slate-900 hover:underline">
                    {l.title}
                  </Link>
                  <div className="mt-0.5 flex flex-wrap items-center gap-2 text-sm text-slate-600">
                    <ListingStatusBadge status={l.status} />
                    {l.locality && <span>{l.locality}</span>}
                    {l.minimumRent !== null && <span>from {formatRupees(l.minimumRent)}/month</span>}
                    <span>· updated {formatDate(l.updatedAt)}</span>
                  </div>
                  {l.statusReason && <p className="mt-1 text-sm text-orange-800">“{l.statusReason}”</p>}
                </div>
                <Link href={`/owner/listings/${l.id}`} className="text-sm text-teal-700 underline underline-offset-4">
                  {l.status === "DRAFT" || l.status === "CHANGES_REQUESTED" ? "Continue editing" : "View"}
                </Link>
              </li>
            ))}
          </ul>
        )}

        <form onSubmit={createListing} className="mt-6 flex flex-col gap-3 border-t border-slate-100 pt-5 sm:flex-row sm:items-end" noValidate>
          <div className="flex-1">
            <TextField
              label="New listing title"
              value={title}
              onChange={setTitle}
              maxLength={120}
              placeholder="e.g. Sunrise PG, Matunga East"
              error={titleError}
            />
          </div>
          <SubmitButton pending={creating}>Create listing</SubmitButton>
        </form>
      </section>

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
          { phase: "Phase 4", title: "Student enquiries", description: "reply to students interested in your listing" },
          { phase: "Phase 8", title: "Owner verification requests", description: "get the “identity checked” badge" },
        ]}
      />
    </div>
  );
}
