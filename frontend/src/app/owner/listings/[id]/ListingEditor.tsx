"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState, type FormEvent, type ReactNode } from "react";
import ListingDetailsView, { StatusHistory } from "@/components/ListingDetailsView";
import ListingStatusBadge from "@/components/ListingStatusBadge";
import LocationMap from "@/components/LocationMap";
import { FormMessage, SelectField, SubmitButton, TextField } from "@/components/form";
import { ApiError, api, errorMessage } from "@/lib/api";
import {
  CHARGE_FREQUENCY_LABELS,
  CHARGE_TYPE_LABELS,
  GENDER_POLICY_LABELS,
  PROPERTY_TYPE_LABELS,
  ROOM_TYPE_LABELS,
  STATUS_EXPLANATIONS,
  optionsFrom,
} from "@/lib/listingLabels";
import type { ChargeFrequency, ChargeType, GenderPolicy, ListingDetail, PropertyType, RoomType } from "@/lib/types";

// ---------- Form state (strings while typing; converted to numbers on save) ----------

type RoomForm = {
  key: string;
  id?: number;
  roomType: RoomType;
  sharingCapacity: string;
  monthlyRent: string;
  securityDeposit: string;
  bedsTotal: string;
  bedsAvailable: string;
};

type ChargeForm = {
  key: string;
  chargeType: ChargeType;
  description: string;
  amount: string;
  frequency: ChargeFrequency;
  mandatory: boolean;
};

type Answer = "yes" | "no" | "unknown";

type FormState = {
  title: string;
  description: string;
  propertyType: PropertyType;
  genderPolicy: GenderPolicy;
  addressLine: string;
  locality: string;
  pincode: string;
  latitude: string;
  longitude: string;
  rooms: RoomForm[];
  charges: ChargeForm[];
  amenities: Record<number, Answer>;
};

const newKey = () => crypto.randomUUID();
const str = (n: number | null | undefined) => (n === null || n === undefined ? "" : String(n));
const num = (s: string) => (s.trim() === "" ? null : Number(s));

function toForm(l: ListingDetail): FormState {
  return {
    title: l.title,
    description: l.description ?? "",
    propertyType: l.propertyType,
    genderPolicy: l.genderPolicy,
    addressLine: l.addressLine ?? "",
    locality: l.locality ?? "",
    pincode: l.pincode ?? "",
    latitude: str(l.latitude),
    longitude: str(l.longitude),
    rooms: l.rooms.map((r) => ({
      key: newKey(),
      id: r.id,
      roomType: r.roomType,
      sharingCapacity: str(r.sharingCapacity),
      monthlyRent: str(r.monthlyRent),
      securityDeposit: str(r.securityDeposit),
      bedsTotal: str(r.bedsTotal),
      bedsAvailable: str(r.bedsAvailable),
    })),
    charges: l.charges.map((c) => ({
      key: newKey(),
      chargeType: c.chargeType,
      description: c.description ?? "",
      amount: str(c.amount),
      frequency: c.frequency,
      mandatory: c.mandatory,
    })),
    amenities: Object.fromEntries(
      l.amenities.map((a) => [a.amenityId, a.available === null ? "unknown" : a.available ? "yes" : "no"]),
    ),
  };
}

function toRequest(f: FormState) {
  return {
    title: f.title,
    description: f.description,
    propertyType: f.propertyType,
    genderPolicy: f.genderPolicy,
    addressLine: f.addressLine,
    locality: f.locality,
    pincode: f.pincode,
    latitude: num(f.latitude),
    longitude: num(f.longitude),
    rooms: f.rooms.map((r) => ({
      id: r.id ?? null,
      roomType: r.roomType,
      sharingCapacity: num(r.sharingCapacity),
      monthlyRent: num(r.monthlyRent),
      securityDeposit: num(r.securityDeposit),
      bedsTotal: num(r.bedsTotal),
      bedsAvailable: num(r.bedsAvailable),
    })),
    charges: f.charges.map((c) => ({
      chargeType: c.chargeType,
      description: c.description,
      amount: num(c.amount),
      frequency: c.frequency,
      mandatory: c.mandatory,
    })),
    amenities: Object.entries(f.amenities)
      .filter(([, answer]) => answer !== "unknown")
      .map(([amenityId, answer]) => ({ amenityId: Number(amenityId), available: answer === "yes" })),
  };
}

const emptyRoom = (): RoomForm => ({
  key: newKey(),
  roomType: "DOUBLE",
  sharingCapacity: "2",
  monthlyRent: "",
  securityDeposit: "",
  bedsTotal: "",
  bedsAvailable: "",
});

const emptyCharge = (): ChargeForm => ({
  key: newKey(),
  chargeType: "ELECTRICITY",
  description: "",
  amount: "",
  frequency: "MONTHLY",
  mandatory: true,
});

// ---------- Page ----------

export default function ListingEditor() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const [listing, setListing] = useState<ListingDetail | null>(null);
  const [form, setForm] = useState<FormState | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [message, setMessage] = useState<{ kind: "error" | "success"; text: string } | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [pending, setPending] = useState<"save" | "submit" | "delete" | null>(null);
  const [confirmDelete, setConfirmDelete] = useState(false);

  useEffect(() => {
    api<ListingDetail>(`/api/v1/owner/listings/${id}`)
      .then((l) => {
        setListing(l);
        setForm(toForm(l));
      })
      .catch((e) => setLoadError(errorMessage(e)));
  }, [id]);

  if (loadError) return <FormMessage kind="error">{loadError}</FormMessage>;
  if (!listing || !form) return <p className="text-slate-600">Loading…</p>;

  const update = (patch: Partial<FormState>) => setForm({ ...form, ...patch });
  const canSubmit = listing.status === "DRAFT" || listing.status === "CHANGES_REQUESTED";

  async function save(): Promise<ListingDetail | null> {
    setMessage(null);
    setFieldErrors({});
    try {
      const saved = await api<ListingDetail>(`/api/v1/owner/listings/${id}`, { method: "PUT", json: toRequest(form!) });
      setListing(saved);
      setForm(toForm(saved));
      return saved;
    } catch (e) {
      setMessage({ kind: "error", text: errorMessage(e) });
      if (e instanceof ApiError) setFieldErrors(e.fields);
      return null;
    }
  }

  async function handleSave(event: FormEvent) {
    event.preventDefault();
    setPending("save");
    const saved = await save();
    if (saved) {
      setMessage({
        kind: "success",
        text: saved.status === "PENDING_VERIFICATION" && listing!.status !== "PENDING_VERIFICATION"
          ? "Saved. Because you changed reviewed details, the listing has been sent for review again."
          : "Saved.",
      });
    }
    setPending(null);
  }

  async function handleSubmitForReview() {
    setPending("submit");
    const saved = await save();
    if (saved) {
      try {
        const submitted = await api<ListingDetail>(`/api/v1/owner/listings/${id}/submit`, { method: "POST" });
        setListing(submitted);
        setForm(toForm(submitted));
        setMessage({ kind: "success", text: "Submitted. An administrator will review your listing." });
      } catch (e) {
        setMessage({ kind: "error", text: errorMessage(e) });
      }
    }
    setPending(null);
  }

  async function handleDelete() {
    setPending("delete");
    try {
      await api<void>(`/api/v1/owner/listings/${id}`, { method: "DELETE" });
      router.push("/owner");
    } catch (e) {
      setMessage({ kind: "error", text: errorMessage(e) });
      setPending(null);
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <Link href="/owner" className="text-sm text-teal-700 underline underline-offset-4">
          ← Your listings
        </Link>
        <div className="mt-3 flex flex-wrap items-center gap-3">
          <h1 className="text-2xl font-semibold text-slate-900">{listing.title}</h1>
          <ListingStatusBadge status={listing.status} />
        </div>
        <p className="mt-1 text-slate-600">{STATUS_EXPLANATIONS[listing.status]}</p>
        {listing.statusReason && (
          <div className="mt-3 rounded-md border border-orange-200 bg-orange-50 px-3 py-2 text-sm text-orange-900">
            <span className="font-medium">Message from CampusNest:</span> {listing.statusReason}
          </div>
        )}
      </div>

      {message && <FormMessage kind={message.kind}>{message.text}</FormMessage>}

      {listing.status.startsWith("PUBLISHED") && (
        <AvailabilityPanel
          key={listing.rooms.map((r) => `${r.id}:${r.bedsAvailable}`).join(",")}
          listing={listing}
          onUpdated={(l) => {
            setListing(l);
            // Keep unsaved form edits, but take the new bed counts.
            setForm((f) => f && {
              ...f,
              rooms: f.rooms.map((r) => {
                const updated = l.rooms.find((x) => x.id === r.id);
                return updated ? { ...r, bedsAvailable: String(updated.bedsAvailable) } : r;
              }),
            });
          }}
        />
      )}

      {!listing.ownerCanEdit ? (
        <Card title="Listing details">
          <ListingDetailsView listing={listing} />
        </Card>
      ) : (
        <form onSubmit={handleSave} className="space-y-6" noValidate>
          {listing.status.startsWith("PUBLISHED") && (
            <p className="rounded-md border border-sky-200 bg-sky-50 px-3 py-2 text-sm text-sky-900">
              Changing the title, description, address, location, property type, gender policy, amenities or photos sends
              the listing back for review. Rent, extra charges and bed counts can be changed at any time.
            </p>
          )}

          <Card title="Basics">
            <div className="space-y-4">
              <TextField label="Title" value={form.title} onChange={(v) => update({ title: v })} maxLength={120} error={fieldErrors.title} />
              <div className="grid gap-4 sm:grid-cols-2">
                <SelectField label="Property type" value={form.propertyType} onChange={(v) => update({ propertyType: v as PropertyType })} options={optionsFrom(PROPERTY_TYPE_LABELS)} />
                <SelectField label="Who can stay" value={form.genderPolicy} onChange={(v) => update({ genderPolicy: v as GenderPolicy })} options={optionsFrom(GENDER_POLICY_LABELS)} />
              </div>
              <div>
                <label htmlFor="description" className="block text-sm font-medium text-slate-800">Description</label>
                <textarea
                  id="description"
                  value={form.description}
                  onChange={(e) => update({ description: e.target.value })}
                  maxLength={4000}
                  rows={5}
                  aria-describedby="description-hint"
                  className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-slate-900 focus:border-teal-600 focus:outline-none focus:ring-2 focus:ring-teal-600/30"
                />
                <p id="description-hint" className="mt-1 text-xs text-slate-500">
                  At least 30 characters. Describe the rooms, house rules and what is nearby. {form.description.length}/4000
                </p>
              </div>
            </div>
          </Card>

          <Card title="Location" description="Click the map to mark the building, or type the coordinates.">
            <div className="space-y-4">
              <TextField label="Street address" value={form.addressLine} onChange={(v) => update({ addressLine: v })} maxLength={300} error={fieldErrors.addressLine} />
              <div className="grid gap-4 sm:grid-cols-2">
                <TextField label="Locality" value={form.locality} onChange={(v) => update({ locality: v })} maxLength={100} hint="For example: Matunga East, Dadar West" />
                <TextField label="PIN code" value={form.pincode} onChange={(v) => update({ pincode: v })} inputMode="numeric" maxLength={6} error={fieldErrors.pincode} />
              </div>
              <LocationMap
                latitude={num(form.latitude)}
                longitude={num(form.longitude)}
                onChange={(lat, lng) => update({ latitude: String(lat), longitude: String(lng) })}
                label="Map. Click to set the property location."
              />
              <div className="grid gap-4 sm:grid-cols-2">
                <TextField label="Latitude" value={form.latitude} onChange={(v) => update({ latitude: v })} inputMode="decimal" error={fieldErrors.latitude} />
                <TextField label="Longitude" value={form.longitude} onChange={(v) => update({ longitude: v })} inputMode="decimal" error={fieldErrors.longitude} />
              </div>
            </div>
          </Card>

          <Card title="Rooms and rent" description="Add each type of room you offer. Rent is per person per month.">
            <div className="space-y-4">
              {form.rooms.length === 0 && <p className="text-sm text-slate-600">No rooms yet.</p>}
              {form.rooms.map((room, i) => {
                const setRoom = (patch: Partial<RoomForm>) =>
                  update({ rooms: form.rooms.map((r) => (r.key === room.key ? { ...r, ...patch } : r)) });
                const err = (field: string) => fieldErrors[`rooms[${i}].${field}`];
                return (
                  <fieldset key={room.key} className="rounded-md border border-slate-200 p-4">
                    <legend className="px-1 text-sm font-medium text-slate-700">Room type {i + 1}</legend>
                    <div className="grid gap-3 sm:grid-cols-3">
                      <SelectField label="Type" value={room.roomType} onChange={(v) => setRoom({ roomType: v as RoomType })} options={optionsFrom(ROOM_TYPE_LABELS)} />
                      <TextField label="People per room" value={room.sharingCapacity} onChange={(v) => setRoom({ sharingCapacity: v })} inputMode="numeric" error={err("sharingCapacity")} />
                      <TextField label="Rent (₹/month)" value={room.monthlyRent} onChange={(v) => setRoom({ monthlyRent: v })} inputMode="numeric" error={err("monthlyRent")} />
                      <TextField label="Deposit (₹, optional)" value={room.securityDeposit} onChange={(v) => setRoom({ securityDeposit: v })} inputMode="numeric" error={err("securityDeposit")} />
                      <TextField label="Total beds" value={room.bedsTotal} onChange={(v) => setRoom({ bedsTotal: v })} inputMode="numeric" error={err("bedsTotal")} />
                      <TextField label="Free beds now" value={room.bedsAvailable} onChange={(v) => setRoom({ bedsAvailable: v })} inputMode="numeric" error={err("bedsAvailable") ?? err("bedsConsistent")} />
                    </div>
                    <button type="button" onClick={() => update({ rooms: form.rooms.filter((r) => r.key !== room.key) })} className="mt-3 text-sm text-red-700 hover:underline">
                      Remove this room type
                    </button>
                  </fieldset>
                );
              })}
              <button type="button" onClick={() => update({ rooms: [...form.rooms, emptyRoom()] })} className="rounded-md border border-slate-300 bg-white px-3 py-1.5 text-sm hover:bg-slate-50">
                + Add room type
              </button>
            </div>
          </Card>

          <Card title="Extra charges" description="Anything students pay on top of rent. Honest charges help students compare true costs.">
            <div className="space-y-4">
              {form.charges.length === 0 && <p className="text-sm text-slate-600">No extra charges.</p>}
              {form.charges.map((charge, i) => {
                const setCharge = (patch: Partial<ChargeForm>) =>
                  update({ charges: form.charges.map((c) => (c.key === charge.key ? { ...c, ...patch } : c)) });
                return (
                  <fieldset key={charge.key} className="rounded-md border border-slate-200 p-4">
                    <legend className="px-1 text-sm font-medium text-slate-700">Charge {i + 1}</legend>
                    <div className="grid gap-3 sm:grid-cols-3">
                      <SelectField label="Type" value={charge.chargeType} onChange={(v) => setCharge({ chargeType: v as ChargeType })} options={optionsFrom(CHARGE_TYPE_LABELS)} />
                      <TextField label="Amount (₹)" value={charge.amount} onChange={(v) => setCharge({ amount: v })} inputMode="numeric" error={fieldErrors[`charges[${i}].amount`]} />
                      <SelectField label="How often" value={charge.frequency} onChange={(v) => setCharge({ frequency: v as ChargeFrequency })} options={optionsFrom(CHARGE_FREQUENCY_LABELS)} />
                    </div>
                    <div className="mt-3 grid gap-3 sm:grid-cols-2">
                      <TextField label="Note (optional)" value={charge.description} onChange={(v) => setCharge({ description: v })} maxLength={100} />
                      <label className="flex items-center gap-2 self-end pb-2 text-sm text-slate-800">
                        <input type="checkbox" checked={charge.mandatory} onChange={(e) => setCharge({ mandatory: e.target.checked })} />
                        Every tenant must pay this
                      </label>
                    </div>
                    <button type="button" onClick={() => update({ charges: form.charges.filter((c) => c.key !== charge.key) })} className="mt-3 text-sm text-red-700 hover:underline">
                      Remove this charge
                    </button>
                  </fieldset>
                );
              })}
              <button type="button" onClick={() => update({ charges: [...form.charges, emptyCharge()] })} className="rounded-md border border-slate-300 bg-white px-3 py-1.5 text-sm hover:bg-slate-50">
                + Add charge
              </button>
            </div>
          </Card>

          <Card title="Amenities" description="Items marked * must be answered before you can submit.">
            <div className="grid gap-x-6 gap-y-3 sm:grid-cols-2">
              {listing.amenities.map((a) => (
                <fieldset key={a.amenityId} className="flex items-center justify-between gap-3 border-b border-slate-100 pb-2">
                  <legend className="sr-only">{a.name}</legend>
                  <span aria-hidden="true" className="text-sm text-slate-800">{a.name}{a.core && " *"}</span>
                  <div className="flex gap-1 text-xs">
                    {(["yes", "no", "unknown"] as Answer[]).map((answer) => {
                      const checked = (form.amenities[a.amenityId] ?? "unknown") === answer;
                      return (
                        <label key={answer} className={`cursor-pointer rounded border px-2 py-1 ${checked ? "border-teal-700 bg-teal-700 text-white" : "border-slate-300 bg-white text-slate-700"}`}>
                          <input
                            type="radio"
                            name={`amenity-${a.amenityId}`}
                            value={answer}
                            checked={checked}
                            onChange={() => update({ amenities: { ...form.amenities, [a.amenityId]: answer } })}
                            className="sr-only"
                          />
                          {answer === "yes" ? "Yes" : answer === "no" ? "No" : "Not sure"}
                        </label>
                      );
                    })}
                  </div>
                </fieldset>
              ))}
            </div>
          </Card>

          <div className="flex flex-wrap items-center gap-3">
            <SubmitButton pending={pending === "save"}>Save changes</SubmitButton>
            {listing.status === "DRAFT" && !confirmDelete && (
              <button type="button" onClick={() => setConfirmDelete(true)} className="text-sm text-red-700 hover:underline">
                Delete draft
              </button>
            )}
            {confirmDelete && (
              <span className="flex items-center gap-2 text-sm">
                Delete this draft permanently?
                <button type="button" onClick={handleDelete} disabled={pending === "delete"} className="rounded bg-red-700 px-2 py-1 text-white">
                  Yes, delete
                </button>
                <button type="button" onClick={() => setConfirmDelete(false)} className="rounded border border-slate-300 px-2 py-1">
                  Cancel
                </button>
              </span>
            )}
          </div>
        </form>
      )}

      {listing.ownerCanEdit && (
        <PhotosPanel
          listing={listing}
          onChanged={(l) => setListing(l)}
          onError={(text) => setMessage({ kind: "error", text })}
        />
      )}

      {canSubmit && (
        <Card title="Submit for review">
          {listing.missingForSubmission.length > 0 ? (
            <>
              <p className="text-sm text-slate-700">Before you can submit, please:</p>
              <ul className="mt-2 list-disc space-y-1 pl-5 text-sm text-slate-700">
                {listing.missingForSubmission.map((item) => (
                  <li key={item}>{item}</li>
                ))}
              </ul>
              <p className="mt-2 text-xs text-slate-500">This checklist updates each time you save.</p>
            </>
          ) : (
            <p className="text-sm text-slate-700">Everything required is filled in. Your latest changes will be saved first.</p>
          )}
          <button
            type="button"
            onClick={handleSubmitForReview}
            disabled={pending !== null}
            className="mt-4 rounded-md bg-teal-700 px-4 py-2 font-medium text-white hover:bg-teal-800 disabled:opacity-60"
          >
            {pending === "submit" ? "Submitting…" : "Save and submit for review"}
          </button>
        </Card>
      )}

      <details className="rounded-lg border border-slate-200 bg-white p-5">
        <summary className="cursor-pointer font-semibold text-slate-900">History</summary>
        <div className="mt-4">
          <StatusHistory history={listing.history} />
        </div>
      </details>
    </div>
  );
}

function Card({ title, description, children }: { title: string; description?: string; children: ReactNode }) {
  return (
    <section className="rounded-lg border border-slate-200 bg-white p-5 sm:p-6">
      <h2 className="text-lg font-semibold text-slate-900">{title}</h2>
      {description && <p className="mt-1 text-sm text-slate-600">{description}</p>}
      <div className="mt-4">{children}</div>
    </section>
  );
}

function PhotosPanel({ listing, onChanged, onError }: { listing: ListingDetail; onChanged: (l: ListingDetail) => void; onError: (text: string) => void }) {
  const [uploading, setUploading] = useState(false);

  async function upload(file: File) {
    setUploading(true);
    const data = new FormData();
    data.append("file", file);
    try {
      onChanged(await api<ListingDetail>(`/api/v1/owner/listings/${listing.id}/images`, { method: "POST", formData: data }));
    } catch (e) {
      onError(errorMessage(e));
    } finally {
      setUploading(false);
    }
  }

  async function remove(imageId: number) {
    try {
      onChanged(await api<ListingDetail>(`/api/v1/owner/listings/${listing.id}/images/${imageId}`, { method: "DELETE" }));
    } catch (e) {
      onError(errorMessage(e));
    }
  }

  return (
    <Card title="Photos" description="JPEG or PNG, up to 5 MB each, maximum 10. Location data inside photos is removed automatically.">
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
        {listing.images.map((img) => (
          <div key={img.id} className="relative">
            {/* eslint-disable-next-line @next/next/no-img-element -- private photos must load with the user's session cookie */}
            <img src={img.url} alt="Listing photo" className="aspect-[4/3] w-full rounded-md border border-slate-200 object-cover" />
            <button type="button" onClick={() => remove(img.id)} className="absolute right-1 top-1 rounded bg-white/90 px-2 py-0.5 text-xs text-red-700 shadow">
              Remove
            </button>
          </div>
        ))}
      </div>
      {listing.images.length < 10 && (
        <label className="mt-4 inline-block cursor-pointer rounded-md border border-slate-300 bg-white px-3 py-1.5 text-sm hover:bg-slate-50">
          {uploading ? "Uploading…" : "+ Add photo"}
          <input
            type="file"
            accept="image/jpeg,image/png"
            className="sr-only"
            disabled={uploading}
            onChange={(e) => {
              const file = e.target.files?.[0];
              e.target.value = "";
              if (file) upload(file);
            }}
          />
        </label>
      )}
    </Card>
  );
}

function AvailabilityPanel({ listing, onUpdated }: { listing: ListingDetail; onUpdated: (l: ListingDetail) => void }) {
  const [beds, setBeds] = useState<Record<number, string>>(
    Object.fromEntries(listing.rooms.map((r) => [r.id, String(r.bedsAvailable)])),
  );
  const [pending, setPending] = useState(false);
  const [message, setMessage] = useState<{ kind: "error" | "success"; text: string } | null>(null);

  async function confirm(event: FormEvent) {
    event.preventDefault();
    setPending(true);
    setMessage(null);
    try {
      const updated = await api<ListingDetail>(`/api/v1/owner/listings/${listing.id}/availability`, {
        method: "PATCH",
        json: { rooms: listing.rooms.map((r) => ({ roomId: r.id, bedsAvailable: num(beds[r.id] ?? "") })) },
      });
      onUpdated(updated);
      setMessage({ kind: "success", text: "Availability confirmed. Students will see it as up to date." });
    } catch (e) {
      setMessage({ kind: "error", text: errorMessage(e) });
    } finally {
      setPending(false);
    }
  }

  return (
    <Card title="Availability" description="Confirm free beds regularly. Listings that are not confirmed for 30 days are shown to students as possibly out of date.">
      <form onSubmit={confirm} className="space-y-3" noValidate>
        {listing.rooms.map((r) => (
          <div key={r.id} className="flex items-end gap-3">
            <div className="flex-1">
              <TextField
                label={`${ROOM_TYPE_LABELS[r.roomType]}: free beds (of ${r.bedsTotal})`}
                value={beds[r.id] ?? ""}
                onChange={(v) => setBeds({ ...beds, [r.id]: v })}
                inputMode="numeric"
              />
            </div>
          </div>
        ))}
        {message && <FormMessage kind={message.kind}>{message.text}</FormMessage>}
        <SubmitButton pending={pending}>Confirm availability</SubmitButton>
      </form>
    </Card>
  );
}
