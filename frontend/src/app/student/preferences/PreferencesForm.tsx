"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";
import { FormMessage, SelectField, SubmitButton, TextField } from "@/components/form";
import { ApiError, api, errorMessage } from "@/lib/api";
import { ROOM_TYPE_LABELS } from "@/lib/listingLabels";
import type { Preferences, RoomType, TravelMode } from "@/lib/types";

type Amenity = { id: number; code: string; name: string; core: boolean };
type Wish = "must" | "nice" | "none";

const MODES: { value: TravelMode; label: string }[] = [
  { value: "ANY", label: "Whatever is practical (walk if close, otherwise auto-rickshaw)" },
  { value: "WALK", label: "Walking" },
  { value: "AUTO_RICKSHAW", label: "Auto-rickshaw" },
  { value: "LOCAL_TRAIN", label: "Local train (estimates coming soon; road estimate used for now)" },
];

const ROOM_OPTIONS = [{ value: "", label: "No choice" }].concat(
  (Object.keys(ROOM_TYPE_LABELS) as RoomType[]).map((r) => ({ value: r, label: ROOM_TYPE_LABELS[r] })),
);

export default function PreferencesForm() {
  const router = useRouter();
  const [amenities, setAmenities] = useState<Amenity[]>([]);
  const [budget, setBudget] = useState("10000");
  const [commute, setCommute] = useState("40");
  const [mode, setMode] = useState<TravelMode>("ANY");
  const [rooms, setRooms] = useState<string[]>(["DOUBLE", "", ""]);
  const [wishes, setWishes] = useState<Record<string, Wish>>({});
  const [loaded, setLoaded] = useState(false);
  const [message, setMessage] = useState<{ kind: "error" | "success"; text: string } | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [pending, setPending] = useState(false);

  useEffect(() => {
    Promise.all([api<Amenity[]>("/api/v1/amenities"), api<Preferences | undefined>("/api/v1/student/preferences")])
      .then(([list, prefs]) => {
        setAmenities(list);
        if (prefs) {
          setBudget(String(prefs.budgetMax));
          setCommute(String(prefs.maxCommuteMinutes));
          setMode(prefs.commuteMode);
          setRooms([0, 1, 2].map((i) => prefs.roomChoices[i] ?? ""));
          const w: Record<string, Wish> = {};
          prefs.mustHaveAmenities.forEach((c) => (w[c] = "must"));
          prefs.niceToHaveAmenities.forEach((c) => (w[c] = "nice"));
          setWishes(w);
        }
        setLoaded(true);
      })
      .catch((e) => setMessage({ kind: "error", text: errorMessage(e) }));
  }, []);

  async function save(event: FormEvent) {
    event.preventDefault();
    setPending(true);
    setMessage(null);
    setFieldErrors({});
    try {
      await api<Preferences>("/api/v1/student/preferences", {
        method: "PUT",
        json: {
          budgetMax: Number(budget),
          maxCommuteMinutes: Number(commute),
          commuteMode: mode,
          roomChoices: rooms.filter(Boolean).filter((r, i, all) => all.indexOf(r) === i),
          mustHaveAmenities: Object.keys(wishes).filter((c) => wishes[c] === "must"),
          niceToHaveAmenities: Object.keys(wishes).filter((c) => wishes[c] === "nice"),
        },
      });
      router.push("/student/recommendations");
    } catch (e) {
      setMessage({ kind: "error", text: errorMessage(e) });
      if (e instanceof ApiError) setFieldErrors(e.fields);
      setPending(false);
    }
  }

  if (!loaded && !message) return <p className="text-slate-600">Loading…</p>;

  return (
    <form onSubmit={save} className="space-y-6" noValidate>
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">What are you looking for?</h1>
        <p className="mt-1 text-slate-600">CampusNest uses these answers to score and rank listings for you. You can change them any time.</p>
      </div>
      {message && <FormMessage kind={message.kind}>{message.text}</FormMessage>}

      <section className="space-y-4 rounded-lg border border-slate-200 bg-white p-5">
        <h2 className="font-semibold text-slate-900">Budget and commute</h2>
        <TextField label="Maximum monthly budget (₹), including electricity and other compulsory charges" value={budget} onChange={setBudget} inputMode="numeric" error={fieldErrors.budgetMax} />
        <TextField label="Longest acceptable one-way commute (minutes)" value={commute} onChange={setCommute} inputMode="numeric" error={fieldErrors.maxCommuteMinutes} />
        <SelectField label="How will you usually travel?" value={mode} onChange={(v) => setMode(v as TravelMode)} options={MODES} />
      </section>

      <section className="space-y-4 rounded-lg border border-slate-200 bg-white p-5">
        <h2 className="font-semibold text-slate-900">Room type, in order of preference</h2>
        <div className="grid gap-3 sm:grid-cols-3">
          {["1st choice", "2nd choice", "3rd choice"].map((label, i) => (
            <SelectField key={label} label={label} value={rooms[i]} onChange={(v) => setRooms(rooms.map((r, j) => (j === i ? v : r)))} options={ROOM_OPTIONS} />
          ))}
        </div>
      </section>

      <section className="rounded-lg border border-slate-200 bg-white p-5">
        <h2 className="font-semibold text-slate-900">Amenities</h2>
        <p className="mt-1 text-sm text-slate-600">“Must have” counts twice as much as “nice to have”.</p>
        <div className="mt-4 space-y-2">
          {amenities.map((a) => (
            <fieldset key={a.code} className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-100 pb-2">
              <legend className="sr-only">{a.name}</legend>
              <span aria-hidden="true" className="text-sm text-slate-800">{a.name}</span>
              <div className="flex gap-1 text-xs">
                {(["must", "nice", "none"] as Wish[]).map((w) => {
                  const checked = (wishes[a.code] ?? "none") === w;
                  return (
                    <label key={w} className={`cursor-pointer rounded border px-2 py-1 ${checked ? "border-teal-700 bg-teal-700 text-white" : "border-slate-300 bg-white text-slate-700"}`}>
                      <input type="radio" name={`wish-${a.code}`} checked={checked} onChange={() => setWishes({ ...wishes, [a.code]: w })} className="sr-only" />
                      {w === "must" ? "Must have" : w === "nice" ? "Nice to have" : "Don't mind"}
                    </label>
                  );
                })}
              </div>
            </fieldset>
          ))}
        </div>
      </section>

      <SubmitButton pending={pending}>Save and see my recommendations</SubmitButton>
    </form>
  );
}
