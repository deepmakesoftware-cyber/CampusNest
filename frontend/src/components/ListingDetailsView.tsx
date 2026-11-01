"use client";

import LocationMap from "@/components/LocationMap";
import {
  CHARGE_FREQUENCY_LABELS,
  CHARGE_TYPE_LABELS,
  GENDER_POLICY_LABELS,
  PROPERTY_TYPE_LABELS,
  ROOM_TYPE_LABELS,
  STATUS_LABELS,
  formatDate,
  formatDateTime,
  formatRupees,
} from "@/lib/listingLabels";
import type { ListingDetail, StatusChangeView } from "@/lib/types";

// Read-only presentation of a listing. Used by the admin review page now, and by the student
// listing page in Phase 4.

export default function ListingDetailsView({ listing }: { listing: ListingDetail }) {
  return (
    <div className="space-y-6">
      <section>
        <h2 className="text-xl font-semibold text-slate-900">{listing.title}</h2>
        <p className="mt-1 text-sm text-slate-600">
          {PROPERTY_TYPE_LABELS[listing.propertyType]} · {GENDER_POLICY_LABELS[listing.genderPolicy]}
          {listing.sampleData && <span className="ml-2 rounded bg-purple-50 px-1.5 py-0.5 text-xs text-purple-800">Sample listing</span>}
        </p>
        {listing.description && <p className="mt-3 whitespace-pre-line text-slate-700">{listing.description}</p>}
      </section>

      {listing.images.length > 0 && (
        <section>
          <h3 className="font-semibold text-slate-900">Photos</h3>
          <div className="mt-2 grid grid-cols-2 gap-2 sm:grid-cols-3">
            {listing.images.map((img) => (
              // Plain <img>: photos may be private (owner/admin only), so they must be loaded with the
              // user's own session cookie rather than through Next.js's server-side image optimizer.
              // eslint-disable-next-line @next/next/no-img-element
              <img key={img.id} src={img.url} alt={`Photo of ${listing.title}`} className="aspect-[4/3] w-full rounded-md border border-slate-200 object-cover" />
            ))}
          </div>
        </section>
      )}

      <section>
        <h3 className="font-semibold text-slate-900">Location</h3>
        <p className="mt-1 text-slate-700">
          {[listing.addressLine, listing.locality, listing.city, listing.pincode].filter(Boolean).join(", ") || "Not provided"}
        </p>
        {listing.latitude !== null && listing.longitude !== null && (
          <div className="mt-3">
            <LocationMap latitude={listing.latitude} longitude={listing.longitude} label={`Map showing the location of ${listing.title}`} />
          </div>
        )}
      </section>

      <section>
        <h3 className="font-semibold text-slate-900">Rooms and rent</h3>
        {listing.rooms.length === 0 ? (
          <p className="mt-1 text-slate-600">No rooms added.</p>
        ) : (
          <div className="mt-2 overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="text-slate-600">
                <tr>
                  <th scope="col" className="py-1 pr-4 font-medium">Room</th>
                  <th scope="col" className="py-1 pr-4 font-medium">Rent / month</th>
                  <th scope="col" className="py-1 pr-4 font-medium">Deposit</th>
                  <th scope="col" className="py-1 pr-4 font-medium">Free beds</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {listing.rooms.map((r) => (
                  <tr key={r.id}>
                    <td className="py-2 pr-4">{ROOM_TYPE_LABELS[r.roomType]} ({r.sharingCapacity} per room)</td>
                    <td className="py-2 pr-4">{formatRupees(r.monthlyRent)}</td>
                    <td className="py-2 pr-4">{r.securityDeposit !== null ? formatRupees(r.securityDeposit) : "—"}</td>
                    <td className="py-2 pr-4">{r.bedsAvailable} of {r.bedsTotal}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <p className="mt-2 text-xs text-slate-500">Availability last confirmed by owner: {formatDate(listing.lastConfirmedAt)}</p>
      </section>

      <section>
        <h3 className="font-semibold text-slate-900">Extra charges</h3>
        {listing.charges.length === 0 ? (
          <p className="mt-1 text-slate-600">None disclosed.</p>
        ) : (
          <ul className="mt-1 space-y-1 text-slate-700">
            {listing.charges.map((c, i) => (
              <li key={i}>
                {CHARGE_TYPE_LABELS[c.chargeType]}
                {c.description && ` (${c.description})`}: {formatRupees(c.amount)} {CHARGE_FREQUENCY_LABELS[c.frequency]}
                {!c.mandatory && <span className="text-slate-500"> · optional</span>}
              </li>
            ))}
          </ul>
        )}
      </section>

      <section>
        <h3 className="font-semibold text-slate-900">Amenities</h3>
        <ul className="mt-2 grid gap-x-6 gap-y-1 text-sm sm:grid-cols-2">
          {listing.amenities.map((a) => (
            <li key={a.amenityId} className="flex justify-between gap-3 border-b border-slate-100 py-1">
              <span className="text-slate-700">{a.name}</span>
              <span className={a.available === true ? "text-green-700" : a.available === false ? "text-slate-500" : "text-amber-700"}>
                {a.available === true ? "Yes" : a.available === false ? "No" : "Not specified"}
              </span>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}

export function StatusHistory({ history }: { history: StatusChangeView[] }) {
  return (
    <ol className="space-y-3 text-sm">
      {history.map((h, i) => (
        <li key={i} className="border-l-2 border-slate-200 pl-3">
          <div className="text-slate-900">
            {h.fromStatus ? `${STATUS_LABELS[h.fromStatus]} → ` : "Created as "}
            <span className="font-medium">{STATUS_LABELS[h.toStatus]}</span>
          </div>
          <div className="text-xs text-slate-500">
            {formatDateTime(h.at)}
            {h.actorRole && ` · by ${h.actorRole === "ADMIN" ? "administrator" : "owner"}`}
          </div>
          {h.reason && <p className="mt-1 text-slate-700">“{h.reason}”</p>}
        </li>
      ))}
    </ol>
  );
}
