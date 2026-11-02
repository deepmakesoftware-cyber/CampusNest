"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";
import { FormMessage, SelectField, SubmitButton, TextField } from "@/components/form";
import { ApiError, api, errorMessage } from "@/lib/api";
import { optionsFrom } from "@/lib/listingLabels";
import { FOOD_LABELS, LOCALITIES, SHARING_LABELS, SLEEP_LABELS, SMOKING_LABELS, STUDY_LABELS } from "@/lib/roommateLabels";
import type { FoodPreference, RoommateProfile, SharingSize, SleepSchedule, Smoking, StudyEnvironment } from "@/lib/types";

const DEFAULTS: RoommateProfile = {
  discoverable: true,
  budgetMin: 7000,
  budgetMax: 11000,
  preferredLocalities: [],
  anywhereNearCollege: true,
  sleepSchedule: "REGULAR",
  smoking: "NON_SMOKER",
  cannotLiveWithSmoker: false,
  studyEnvironment: "MODERATE",
  cleanliness: 3,
  guests: 3,
  foodPreference: null,
  sharingSize: "EITHER",
  sameGenderOnly: false,
  about: "",
};

function Scale({ label, value, onChange, low, high }: { label: string; value: number; onChange: (v: number) => void; low: string; high: string }) {
  return (
    <fieldset>
      <legend className="text-sm font-medium text-slate-800">{label}</legend>
      <div className="mt-1 flex items-center gap-2 text-xs text-slate-500">
        <span>{low}</span>
        {[1, 2, 3, 4, 5].map((n) => (
          <label key={n} className={`cursor-pointer rounded border px-2.5 py-1 text-sm ${value === n ? "border-teal-700 bg-teal-700 text-white" : "border-slate-300 bg-white text-slate-700"}`}>
            <input type="radio" checked={value === n} onChange={() => onChange(n)} className="sr-only" />
            {n}
          </label>
        ))}
        <span>{high}</span>
      </div>
    </fieldset>
  );
}

export default function RoommateProfileForm() {
  const router = useRouter();
  const [p, setP] = useState<RoommateProfile>(DEFAULTS);
  const [min, setMin] = useState(String(DEFAULTS.budgetMin));
  const [max, setMax] = useState(String(DEFAULTS.budgetMax));
  const [loaded, setLoaded] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [pending, setPending] = useState(false);
  const set = (patch: Partial<RoommateProfile>) => setP({ ...p, ...patch });

  useEffect(() => {
    api<RoommateProfile | undefined>("/api/v1/student/roommates/profile")
      .then((existing) => {
        if (existing) {
          setP({ ...existing, about: existing.about ?? "" });
          setMin(String(existing.budgetMin));
          setMax(String(existing.budgetMax));
        }
        setLoaded(true);
      })
      .catch((e) => setMessage(errorMessage(e)));
  }, []);

  async function save(event: FormEvent) {
    event.preventDefault();
    setPending(true);
    setMessage(null);
    setFieldErrors({});
    try {
      await api("/api/v1/student/roommates/profile", { method: "PUT", json: { ...p, budgetMin: Number(min), budgetMax: Number(max) } });
      router.push("/student/roommates");
    } catch (e) {
      setMessage(errorMessage(e));
      if (e instanceof ApiError) setFieldErrors(e.fields);
      setPending(false);
    }
  }

  if (!loaded && !message) return <p className="text-slate-600">Loading…</p>;

  return (
    <form onSubmit={save} className="space-y-6" noValidate>
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">Your roommate profile</h1>
        <p className="mt-1 text-slate-600">
          Answer honestly: matches are only as good as the answers. Other students never see your email, phone, full name
          or exact budget unless you both accept a connection.
        </p>
      </div>
      {message && <FormMessage kind="error">{message}</FormMessage>}

      <section className="space-y-4 rounded-lg border border-slate-200 bg-white p-5">
        <label className="flex items-start gap-2 text-sm text-slate-800">
          <input type="checkbox" checked={p.discoverable} onChange={(e) => set({ discoverable: e.target.checked })} className="mt-0.5" />
          <span><span className="font-medium">Show me to other students looking for roommates</span><br />
            <span className="text-slate-500">Untick to hide yourself at any time. You can only see others while you are visible.</span></span>
        </label>
      </section>

      <section className="space-y-4 rounded-lg border border-slate-200 bg-white p-5">
        <h2 className="font-semibold text-slate-900">Budget and area</h2>
        <div className="grid gap-4 sm:grid-cols-2">
          <TextField label="Minimum rent you'd pay (₹/month)" value={min} onChange={setMin} inputMode="numeric" error={fieldErrors.budgetMin} />
          <TextField label="Maximum rent (₹/month)" value={max} onChange={setMax} inputMode="numeric" error={fieldErrors.budgetMax} />
        </div>
        <label className="flex items-center gap-2 text-sm text-slate-800">
          <input type="checkbox" checked={p.anywhereNearCollege} onChange={(e) => set({ anywhereNearCollege: e.target.checked })} />
          Anywhere near my college is fine
        </label>
        {!p.anywhereNearCollege && (
          <fieldset>
            <legend className="text-sm font-medium text-slate-800">Preferred areas</legend>
            <div className="mt-2 flex flex-wrap gap-2">
              {LOCALITIES.map((loc) => {
                const on = p.preferredLocalities.includes(loc);
                return (
                  <label key={loc} className={`cursor-pointer rounded-full border px-3 py-1 text-sm ${on ? "border-teal-700 bg-teal-700 text-white" : "border-slate-300 bg-white text-slate-700"}`}>
                    <input type="checkbox" checked={on} className="sr-only"
                      onChange={() => set({ preferredLocalities: on ? p.preferredLocalities.filter((x) => x !== loc) : [...p.preferredLocalities, loc] })} />
                    {loc}
                  </label>
                );
              })}
            </div>
          </fieldset>
        )}
      </section>

      <section className="space-y-4 rounded-lg border border-slate-200 bg-white p-5">
        <h2 className="font-semibold text-slate-900">Daily habits</h2>
        <SelectField label="When do you usually sleep?" value={p.sleepSchedule} onChange={(v) => set({ sleepSchedule: v as SleepSchedule })} options={optionsFrom(SLEEP_LABELS)} />
        <SelectField label="How do you like to study at home?" value={p.studyEnvironment} onChange={(v) => set({ studyEnvironment: v as StudyEnvironment })} options={optionsFrom(STUDY_LABELS)} />
        <Scale label="How tidy do you keep shared spaces?" value={p.cleanliness} onChange={(v) => set({ cleanliness: v })} low="Relaxed" high="Very tidy" />
        <Scale label="How often do you have friends over?" value={p.guests} onChange={(v) => set({ guests: v })} low="Rarely" high="Often" />
        <SelectField label="Smoking" value={p.smoking} onChange={(v) => set({ smoking: v as Smoking })} options={optionsFrom(SMOKING_LABELS)} />
        <label className="flex items-center gap-2 text-sm text-slate-800">
          <input type="checkbox" checked={p.cannotLiveWithSmoker} onChange={(e) => set({ cannotLiveWithSmoker: e.target.checked })} />
          I cannot live with someone who smokes (a dealbreaker)
        </label>
        <SelectField label="Food habits (optional)" value={p.foodPreference ?? ""} onChange={(v) => set({ foodPreference: (v || null) as FoodPreference | null })}
          options={[{ value: "", label: "Prefer not to say" }, ...optionsFrom(FOOD_LABELS)]} hint="Leave this out and it won't count either way." />
        <SelectField label="How many roommates would you like?" value={p.sharingSize} onChange={(v) => set({ sharingSize: v as SharingSize })} options={optionsFrom(SHARING_LABELS)} />
        <label className="flex items-start gap-2 text-sm text-slate-800">
          <input type="checkbox" checked={p.sameGenderOnly} onChange={(e) => set({ sameGenderOnly: e.target.checked })} className="mt-0.5" />
          <span>Only match me with students of my gender<br /><span className="text-slate-500">Uses the optional gender in your account profile. Your gender is never shown to others.</span></span>
        </label>
      </section>

      <section className="rounded-lg border border-slate-200 bg-white p-5">
        <TextField label="A line about you (optional)" value={p.about ?? ""} onChange={(v) => set({ about: v })} maxLength={300}
          hint="For example your course or routine. Please don't add phone numbers or social media handles here." />
      </section>

      <SubmitButton pending={pending}>Save and see matches</SubmitButton>
    </form>
  );
}
