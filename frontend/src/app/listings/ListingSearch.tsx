"use client";

import Link from "next/link";
import { useEffect, useState, type FormEvent } from "react";
import ListingsMap from "@/components/ListingsMap";
import { FreshnessNote, OwnerCheckedBadge, SampleBadge } from "@/components/TrustBadges";
import { FormMessage } from "@/components/form";
import { api, errorMessage } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { GENDER_POLICY_LABELS, PROPERTY_TYPE_LABELS, ROOM_TYPE_LABELS, formatRupees } from "@/lib/listingLabels";
import type { College, ListingCard, Page, RoomType, SortOrder, StudentProfile } from "@/lib/types";

type Amenity = { id: number; code: string; name: string; core: boolean };

type Filters = {
  q: string;
  maxRent: string;
  roomType: RoomType | "";
  suitableFor: "" | "FEMALE" | "MALE";
  amenities: string[];
  availableOnly: boolean;
  maxDistanceKm: string;
  sort: SortOrder;
};

const INITIAL: Filters = {
  q: "",
  maxRent: "",
  roomType: "",
  suitableFor: "",
  amenities: [],
  availableOnly: true,
  maxDistanceKm: "",
  sort: "DISTANCE_ASC",
};

const selectClass = "mt-1 block w-full rounded-md border border-slate-300 bg-white px-2 py-1.5 text-sm";

export default function ListingSearch() {
  const { state } = useAuth();
  const [colleges, setColleges] = useState<College[]>([]);
  const [collegeId, setCollegeId] = useState<number | null>(null);
  const [amenities, setAmenities] = useState<Amenity[]>([]);
  const [filters, setFilters] = useState<Filters>(INITIAL);
  const [draftQuery, setDraftQuery] = useState("");
  const [page, setPage] = useState(0);
  const [results, setResults] = useState<Page<ListingCard> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [view, setView] = useState<"list" | "map">("list");

  const isStudent = state.status === "authenticated" && state.user.role === "STUDENT";

  // Reference data, and the student's own college if they have set one.
  useEffect(() => {
    if (state.status === "loading") return;
    Promise.all([
      api<College[]>("/api/v1/colleges"),
      api<Amenity[]>("/api/v1/amenities"),
      isStudent ? api<StudentProfile>("/api/v1/student/profile").catch(() => null) : Promise.resolve(null),
    ])
      .then(([collegeList, amenityList, profile]) => {
        setColleges(collegeList);
        setAmenities(amenityList);
        setCollegeId(profile?.collegeId ?? collegeList[0]?.id ?? null);
      })
      .catch((e) => setError(errorMessage(e)));
  }, [state.status, isStudent]);

  useEffect(() => {
    if (collegeId === null) return;
    const params = new URLSearchParams({ collegeId: String(collegeId), sort: filters.sort, page: String(page) });
    if (filters.q) params.set("q", filters.q);
    if (filters.maxRent) params.set("maxRent", filters.maxRent);
    if (filters.roomType) params.set("roomType", filters.roomType);
    if (filters.suitableFor) params.set("suitableFor", filters.suitableFor);
    if (filters.availableOnly) params.set("availableOnly", "true");
    if (filters.maxDistanceKm) params.set("maxDistanceKm", filters.maxDistanceKm);
    filters.amenities.forEach((a) => params.append("amenities", a));
    api<Page<ListingCard>>(`/api/v1/listings?${params}`)
      .then((r) => {
        setResults(r);
        setError(null);
      })
      .catch((e) => setError(errorMessage(e)));
  }, [collegeId, filters, page]);

  const set = (patch: Partial<Filters>) => {
    setFilters({ ...filters, ...patch });
    setPage(0);
  };
  const college = colleges.find((c) => c.id === collegeId) ?? null;

  function submitSearch(event: FormEvent) {
    event.preventDefault();
    set({ q: draftQuery.trim() });
  }

  return (
    <div className="mt-6 grid gap-6 lg:grid-cols-[16rem_1fr]">
      <aside aria-label="Filters" className="space-y-4 rounded-lg border border-slate-200 bg-white p-4 text-sm lg:self-start">
        <form onSubmit={submitSearch}>
          <label htmlFor="q" className="block font-medium text-slate-800">Search</label>
          <div className="mt-1 flex gap-2">
            <input id="q" value={draftQuery} onChange={(e) => setDraftQuery(e.target.value)} placeholder="Area or name" className="min-w-0 flex-1 rounded-md border border-slate-300 px-2 py-1.5" />
            <button type="submit" className="rounded-md bg-teal-700 px-3 text-white">Go</button>
          </div>
        </form>

        <div>
          <label htmlFor="college" className="block font-medium text-slate-800">Distance from</label>
          <select id="college" value={collegeId ?? ""} onChange={(e) => { setCollegeId(Number(e.target.value)); setPage(0); }} className={selectClass}>
            {colleges.map((c) => <option key={c.id} value={c.id}>{c.shortName}</option>)}
          </select>
        </div>

        <div>
          <label htmlFor="maxRent" className="block font-medium text-slate-800">Max rent per month</label>
          <select id="maxRent" value={filters.maxRent} onChange={(e) => set({ maxRent: e.target.value })} className={selectClass}>
            <option value="">Any</option>
            {[6000, 8000, 10000, 12000, 15000, 20000].map((v) => <option key={v} value={v}>Up to {formatRupees(v)}</option>)}
          </select>
        </div>

        <div>
          <label htmlFor="distance" className="block font-medium text-slate-800">Max distance</label>
          <select id="distance" value={filters.maxDistanceKm} onChange={(e) => set({ maxDistanceKm: e.target.value })} className={selectClass}>
            <option value="">Any</option>
            {[1, 2, 5, 10, 20].map((v) => <option key={v} value={v}>Within {v} km</option>)}
          </select>
        </div>

        <div>
          <label htmlFor="roomType" className="block font-medium text-slate-800">Room type</label>
          <select id="roomType" value={filters.roomType} onChange={(e) => set({ roomType: e.target.value as RoomType | "" })} className={selectClass}>
            <option value="">Any</option>
            {(Object.keys(ROOM_TYPE_LABELS) as RoomType[]).map((r) => <option key={r} value={r}>{ROOM_TYPE_LABELS[r]}</option>)}
          </select>
        </div>

        <div>
          <label htmlFor="suitable" className="block font-medium text-slate-800">Accommodation for</label>
          <select id="suitable" value={filters.suitableFor} onChange={(e) => set({ suitableFor: e.target.value as Filters["suitableFor"] })} className={selectClass}>
            <option value="">Show all</option>
            <option value="FEMALE">Women (hide men-only)</option>
            <option value="MALE">Men (hide women-only)</option>
          </select>
        </div>

        <fieldset>
          <legend className="font-medium text-slate-800">Must have</legend>
          <div className="mt-1 space-y-1">
            {amenities.filter((a) => a.core).map((a) => (
              <label key={a.code} className="flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={filters.amenities.includes(a.code)}
                  onChange={(e) => set({ amenities: e.target.checked ? [...filters.amenities, a.code] : filters.amenities.filter((x) => x !== a.code) })}
                />
                {a.name}
              </label>
            ))}
          </div>
        </fieldset>

        <label className="flex items-center gap-2">
          <input type="checkbox" checked={filters.availableOnly} onChange={(e) => set({ availableOnly: e.target.checked })} />
          Only with free beds
        </label>

        <button type="button" onClick={() => { setFilters(INITIAL); setDraftQuery(""); setPage(0); }} className="text-teal-700 underline underline-offset-4">
          Clear filters
        </button>
      </aside>

      <section aria-live="polite">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <p className="text-sm text-slate-600">
            {results ? `${results.totalItems} ${results.totalItems === 1 ? "listing" : "listings"}` : "Loading…"}
            {college && ` · distances from ${college.shortName}`}
          </p>
          <div className="flex items-center gap-3 text-sm">
            <label htmlFor="sort" className="text-slate-600">Sort</label>
            <select id="sort" value={filters.sort} onChange={(e) => set({ sort: e.target.value as SortOrder })} className="rounded-md border border-slate-300 bg-white px-2 py-1">
              <option value="DISTANCE_ASC">Nearest first</option>
              <option value="RENT_ASC">Lowest rent first</option>
              <option value="RECENTLY_CONFIRMED">Recently confirmed</option>
            </select>
            <div className="flex overflow-hidden rounded-md border border-slate-300" role="group" aria-label="View">
              {(["list", "map"] as const).map((v) => (
                <button key={v} type="button" aria-pressed={view === v} onClick={() => setView(v)} className={`px-3 py-1 ${view === v ? "bg-slate-800 text-white" : "bg-white"}`}>
                  {v === "list" ? "List" : "Map"}
                </button>
              ))}
            </div>
          </div>
        </div>

        {error && <div className="mt-4"><FormMessage kind="error">{error}</FormMessage></div>}

        {results && results.items.length === 0 && (
          <div className="mt-8 rounded-lg border border-dashed border-slate-300 bg-white p-8 text-center">
            <p className="font-medium text-slate-900">No listings match these filters.</p>
            <p className="mt-1 text-sm text-slate-600">Try a higher rent limit, a larger distance, or fewer “must have” amenities.</p>
          </div>
        )}

        {results && results.items.length > 0 && view === "map" && (
          <div className="mt-4">
            <ListingsMap listings={results.items} college={college} />
            <p className="mt-2 text-xs text-slate-500">Square: college. Grey dots: currently full. Showing this page of results.</p>
          </div>
        )}

        {results && results.items.length > 0 && view === "list" && (
          <ul className="mt-4 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            {results.items.map((l) => <ResultCard key={l.id} listing={l} />)}
          </ul>
        )}

        {results && results.totalPages > 1 && (
          <div className="mt-6 flex items-center gap-3 text-sm">
            <button type="button" disabled={page === 0} onClick={() => setPage(page - 1)} className="rounded border border-slate-300 px-3 py-1 disabled:opacity-50">Previous</button>
            <span className="text-slate-600">Page {results.page + 1} of {results.totalPages}</span>
            <button type="button" disabled={page + 1 >= results.totalPages} onClick={() => setPage(page + 1)} className="rounded border border-slate-300 px-3 py-1 disabled:opacity-50">Next</button>
          </div>
        )}
      </section>
    </div>
  );
}

function ResultCard({ listing: l }: { listing: ListingCard }) {
  return (
    <li className="overflow-hidden rounded-lg border border-slate-200 bg-white">
      <Link href={`/listings/${l.id}`} className="block hover:bg-slate-50">
        {l.coverImageUrl ? (
          // eslint-disable-next-line @next/next/no-img-element -- photos are served by our own API
          <img src={l.coverImageUrl} alt="" className="aspect-[4/3] w-full object-cover" />
        ) : (
          <div className="flex aspect-[4/3] items-center justify-center bg-slate-100 text-sm text-slate-500">No photo</div>
        )}
        <div className="space-y-1 p-3">
          <div className="flex flex-wrap gap-1">
            {l.sampleData && <SampleBadge />}
            <OwnerCheckedBadge status={l.ownerVerification} />
          </div>
          <h2 className="font-semibold text-slate-900">{l.title}</h2>
          <p className="text-sm text-slate-600">
            {l.locality}
            {l.distanceKm !== null && ` · ${l.distanceKm} km away`}
          </p>
          <p className="text-sm text-slate-600">
            {PROPERTY_TYPE_LABELS[l.propertyType]} · {GENDER_POLICY_LABELS[l.genderPolicy]}
          </p>
          <p className="text-lg font-semibold text-slate-900">
            from {formatRupees(l.fromRent)}<span className="text-sm font-normal text-slate-500">/month</span>
          </p>
          <p className="text-sm text-slate-600">{l.bedsAvailable > 0 ? `${l.bedsAvailable} bed${l.bedsAvailable === 1 ? "" : "s"} free` : "Currently full"}</p>
          <FreshnessNote freshness={l.freshness} />
        </div>
      </Link>
    </li>
  );
}
