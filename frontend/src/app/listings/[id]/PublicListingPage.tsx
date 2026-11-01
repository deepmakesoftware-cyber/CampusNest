"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";
import { CompareToggle } from "@/components/CompareControls";
import ListingDetailsView from "@/components/ListingDetailsView";
import ScoreBreakdown, { ScoreBadge } from "@/components/ScoreBreakdown";
import { FreshnessNote, OwnerCheckedBadge, SampleBadge } from "@/components/TrustBadges";
import { FormMessage } from "@/components/form";
import { ApiError, api, errorMessage } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { formatDate } from "@/lib/listingLabels";
import type { EnquiryView, PublicListingDetail, RecommendedListing, SavedListingView } from "@/lib/types";

const OWNER_TYPE = { INDIVIDUAL: "Individual owner", PG_OPERATOR: "PG operator", HOSTEL_OPERATOR: "Hostel operator" } as const;

export default function PublicListingPage() {
  const { id } = useParams<{ id: string }>();
  const { state } = useAuth();
  const [listing, setListing] = useState<PublicListingDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const isStudent = state.status === "authenticated" && state.user.role === "STUDENT";

  useEffect(() => {
    api<PublicListingDetail>(`/api/v1/listings/${id}`)
      .then(setListing)
      .catch((e) => setError(errorMessage(e)));
  }, [id]);

  if (error) {
    return (
      <div className="py-12 text-center">
        <p className="font-medium text-slate-900">{error}</p>
        <p className="mt-1 text-slate-600">It may have been removed, or it is not published.</p>
        <Link href="/listings" className="mt-4 inline-block text-teal-700 underline underline-offset-4">Back to search</Link>
      </div>
    );
  }
  if (!listing) return <p className="text-slate-600">Loading…</p>;

  return (
    <div className="space-y-4">
      <Link href="/listings" className="text-sm text-teal-700 underline underline-offset-4">← All listings</Link>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">{listing.title}</h1>
          <div className="mt-1 flex flex-wrap items-center gap-2">
            {listing.sampleData && <SampleBadge />}
            <OwnerCheckedBadge status={listing.owner.verificationStatus} />
            {listing.status === "PUBLISHED_UNAVAILABLE" && (
              <span className="rounded bg-sky-50 px-1.5 py-0.5 text-xs font-medium text-sky-800">Currently full</span>
            )}
          </div>
        </div>
        {isStudent && (
          <div className="flex items-center gap-2">
            <CompareToggle listingId={listing.id} />
            <SaveButton listingId={listing.id} />
          </div>
        )}
      </div>

      {listing.sampleData && (
        <p className="rounded-md border border-purple-200 bg-purple-50 px-3 py-2 text-sm text-purple-900">
          This is a sample listing created to demonstrate CampusNest. It is not a real property and cannot be booked.
        </p>
      )}

      <div className="grid gap-6 lg:grid-cols-[1fr_20rem]">
        <div className="rounded-lg border border-slate-200 bg-white p-5 sm:p-6">
          <ListingDetailsView listing={listing} hideTitle />
        </div>

        <aside className="space-y-4">
          {isStudent && <SuitabilityPanel listingId={listing.id} />}

          <section className="rounded-lg border border-slate-200 bg-white p-5 text-sm">
            <h2 className="font-semibold text-slate-900">About this listing</h2>
            <ul className="mt-2 space-y-1 text-slate-700">
              <li>Listed by {listing.owner.displayName}{listing.owner.businessName && ` (${listing.owner.businessName})`}</li>
              {listing.owner.ownerType && <li>{OWNER_TYPE[listing.owner.ownerType]}</li>}
              <li>Reviewed by CampusNest before publishing{listing.publishedAt && ` · ${formatDate(listing.publishedAt)}`}</li>
              <li><FreshnessNote freshness={listing.freshness} /> ({formatDate(listing.lastConfirmedAt)})</li>
            </ul>
            <p className="mt-3 text-xs text-slate-500">
              CampusNest reviews listings and some owners&apos; identities, but cannot guarantee a property&apos;s condition or safety.
              Always visit before paying any money.
            </p>
          </section>

          <ContactPanel listing={listing} />
        </aside>
      </div>
    </div>
  );
}

function SuitabilityPanel({ listingId }: { listingId: number }) {
  const [data, setData] = useState<RecommendedListing | null>(null);
  const [needsSetup, setNeedsSetup] = useState(false);

  useEffect(() => {
    api<RecommendedListing>(`/api/v1/student/recommendations/listings/${listingId}`)
      .then(setData)
      .catch((e) => setNeedsSetup(e instanceof ApiError && e.status === 409));
  }, [listingId]);

  if (needsSetup) {
    return (
      <section className="rounded-lg border border-teal-200 bg-teal-50 p-5 text-sm">
        <h2 className="font-semibold text-slate-900">How well does this fit you?</h2>
        <p className="mt-1 text-slate-700">Set your college and preferences to see your personal suitability score.</p>
        <Link href="/student/preferences" className="mt-2 inline-block text-teal-800 underline underline-offset-4">Set preferences</Link>
      </section>
    );
  }
  if (!data) return null;
  const r = data.result;
  return (
    <section className="rounded-lg border border-slate-200 bg-white p-5">
      <h2 className="mb-3 font-semibold text-slate-900">Your suitability</h2>
      {r.eligible && r.totalScore !== null && r.band && <div className="mb-3"><ScoreBadge score={r.totalScore} band={r.band} /></div>}
      <ScoreBreakdown result={r} />
    </section>
  );
}

function SaveButton({ listingId }: { listingId: number }) {
  const [saved, setSaved] = useState<boolean | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api<SavedListingView[]>("/api/v1/student/saved")
      .then((list) => setSaved(list.some((s) => s.listingId === listingId)))
      .catch(() => setSaved(false));
  }, [listingId]);

  async function toggle() {
    setBusy(true);
    try {
      await api<void>(`/api/v1/student/saved/${listingId}`, { method: saved ? "DELETE" : "PUT" });
      setSaved(!saved);
    } finally {
      setBusy(false);
    }
  }

  if (saved === null) return null;
  return (
    <button type="button" onClick={toggle} disabled={busy} aria-pressed={saved} className={`rounded-md border px-3 py-1.5 text-sm font-medium ${saved ? "border-teal-700 bg-teal-50 text-teal-800" : "border-slate-300 bg-white text-slate-800"}`}>
      {saved ? "★ Saved" : "☆ Save"}
    </button>
  );
}

function ContactPanel({ listing }: { listing: PublicListingDetail }) {
  const { state } = useAuth();

  if (state.status !== "authenticated") {
    return (
      <section className="rounded-lg border border-slate-200 bg-white p-5 text-sm">
        <h2 className="font-semibold text-slate-900">Interested?</h2>
        <p className="mt-1 text-slate-600">Log in as a student to message the owner privately.</p>
        <Link href="/login" className="mt-3 inline-block rounded-md bg-teal-700 px-3 py-1.5 font-medium text-white">Log in</Link>
      </section>
    );
  }
  if (state.user.role !== "STUDENT") return null;
  return (
    <section className="rounded-lg border border-slate-200 bg-white p-5 text-sm">
      {listing.sampleData && (
        <p className="mb-2 text-slate-600">This is a sample listing; your message goes to a sample owner account.</p>
      )}
      <EnquiryForm listingId={listing.id} />
    </section>
  );
}

function EnquiryForm({ listingId }: { listingId: number }) {
  const router = useRouter();
  const [message, setMessage] = useState("");
  const [share, setShare] = useState(false);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function send(event: FormEvent) {
    event.preventDefault();
    setPending(true);
    setError(null);
    try {
      const thread = await api<EnquiryView>("/api/v1/student/enquiries", {
        method: "POST",
        json: { listingId, message, shareContact: share },
      });
      router.push(`/messages/${thread.id}`);
    } catch (e) {
      setError(errorMessage(e));
      setPending(false);
    }
  }

  return (
    <form onSubmit={send} className="space-y-2">
      <h2 className="font-semibold text-slate-900">Message the owner</h2>
      <label htmlFor="enquiry" className="sr-only">Your message</label>
      <textarea id="enquiry" value={message} onChange={(e) => setMessage(e.target.value)} rows={4} maxLength={2000} placeholder="Hi, is a bed available from June? Is food included?" className="block w-full rounded-md border border-slate-300 px-3 py-2" />
      <label className="flex items-start gap-2 text-slate-700">
        <input type="checkbox" checked={share} onChange={(e) => setShare(e.target.checked)} className="mt-0.5" />
        Share my email and phone with this owner
      </label>
      <p className="text-xs text-slate-500">If you don&apos;t tick this, the owner only sees your name and can reply here.</p>
      {error && <FormMessage kind="error">{error}</FormMessage>}
      <button type="submit" disabled={pending || !message.trim()} className="w-full rounded-md bg-teal-700 px-3 py-2 font-medium text-white disabled:opacity-50">
        {pending ? "Sending…" : "Send message"}
      </button>
    </form>
  );
}
