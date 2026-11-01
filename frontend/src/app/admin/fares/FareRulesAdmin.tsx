"use client";

import { useEffect, useState, type FormEvent } from "react";
import { FormMessage, SelectField, SubmitButton, TextField } from "@/components/form";
import { ApiError, api, errorMessage } from "@/lib/api";
import { formatDate } from "@/lib/listingLabels";
import type { FareRuleView } from "@/lib/types";

type Kind = FareRuleView["kind"];
const KIND_FOR_MODE: Record<string, Kind> = {
  AUTO_RICKSHAW: "PER_TRIP_METERED",
  BUS: "PER_TRIP_SLAB",
  LOCAL_TRAIN: "MONTHLY_PASS_SLAB",
};
const MODE_OPTIONS = [
  { value: "LOCAL_TRAIN", label: "Local train: monthly pass by rail distance" },
  { value: "BUS", label: "Bus: fare per trip by distance slab" },
  { value: "AUTO_RICKSHAW", label: "Auto-rickshaw: metered fare" },
];

function describe(f: FareRuleView) {
  if (f.kind === "PER_TRIP_METERED") return `₹${f.baseFare} for first ${f.baseKm} km, then ₹${f.perKm}/km`;
  if (f.kind === "PER_TRIP_SLAB") return `₹${f.baseFare} per trip up to ${f.upToKm} km`;
  return `₹${f.baseFare} monthly pass up to ${f.upToKm} km`;
}

export default function FareRulesAdmin() {
  const [rules, setRules] = useState<FareRuleView[]>([]);
  const [mode, setMode] = useState("LOCAL_TRAIN");
  const [upToKm, setUpToKm] = useState("");
  const [baseFare, setBaseFare] = useState("");
  const [baseKm, setBaseKm] = useState("");
  const [perKm, setPerKm] = useState("");
  const [description, setDescription] = useState("");
  const [sourceName, setSourceName] = useState("");
  const [sourceUrl, setSourceUrl] = useState("");
  const [effectiveFrom, setEffectiveFrom] = useState("");
  const [message, setMessage] = useState<{ kind: "error" | "success"; text: string } | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [pending, setPending] = useState(false);
  const metered = mode === "AUTO_RICKSHAW";

  const load = () => api<FareRuleView[]>("/api/v1/fare-rules").then(setRules).catch((e) => setMessage({ kind: "error", text: errorMessage(e) }));
  useEffect(() => {
    load();
  }, []);

  async function add(event: FormEvent) {
    event.preventDefault();
    setPending(true);
    setMessage(null);
    setFieldErrors({});
    try {
      await api<FareRuleView>("/api/v1/admin/fare-rules", {
        method: "POST",
        json: {
          mode,
          kind: KIND_FOR_MODE[mode],
          upToKm: metered ? null : Number(upToKm) || null,
          baseFare: Number(baseFare),
          baseKm: metered ? Number(baseKm) || null : null,
          perKm: metered ? Number(perKm) || null : null,
          description,
          sourceName,
          sourceUrl: sourceUrl || null,
          effectiveFrom: effectiveFrom || null,
        },
      });
      setMessage({ kind: "success", text: "Fare rule added. Comparisons use it immediately." });
      setUpToKm(""); setBaseFare(""); setBaseKm(""); setPerKm(""); setDescription("");
      await load();
    } catch (e) {
      setMessage({ kind: "error", text: errorMessage(e) });
      if (e instanceof ApiError) setFieldErrors(e.fields);
    } finally {
      setPending(false);
    }
  }

  async function remove(id: number) {
    try {
      await api<void>(`/api/v1/admin/fare-rules/${id}`, { method: "DELETE" });
      await load();
    } catch (e) {
      setMessage({ kind: "error", text: errorMessage(e) });
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">Fare rules</h1>
        <p className="mt-1 text-slate-600">
          Transport cost estimates use only these rules. Enter fares only from an official or clearly cited source, with the date they apply from.
        </p>
      </div>

      <ul className="divide-y divide-slate-100 rounded-lg border border-slate-200 bg-white text-sm">
        {rules.length === 0 && <li className="px-4 py-3 text-slate-600">No fare rules.</li>}
        {rules.map((f) => (
          <li key={f.id} className="flex flex-col gap-1 px-4 py-3 sm:flex-row sm:items-start sm:justify-between">
            <div>
              <div className="font-medium text-slate-900">{describe(f)}</div>
              <div className="text-slate-600">{f.description}</div>
              <div className="text-xs text-slate-500">
                Source: {f.sourceUrl ? <a href={f.sourceUrl} target="_blank" rel="noopener noreferrer" className="underline">{f.sourceName}</a> : f.sourceName} · from {formatDate(f.effectiveFrom)}
              </div>
            </div>
            <button type="button" onClick={() => remove(f.id)} className="text-red-700 hover:underline">Remove</button>
          </li>
        ))}
      </ul>

      <form onSubmit={add} className="space-y-4 rounded-lg border border-slate-200 bg-white p-5" noValidate>
        <h2 className="font-semibold text-slate-900">Add a fare rule</h2>
        {message && <FormMessage kind={message.kind}>{message.text}</FormMessage>}
        <SelectField label="Type" value={mode} onChange={setMode} options={MODE_OPTIONS} />
        <div className="grid gap-4 sm:grid-cols-3">
          {metered ? (
            <>
              <TextField label="Minimum fare (₹)" value={baseFare} onChange={setBaseFare} inputMode="decimal" error={fieldErrors.baseFare} />
              <TextField label="Km covered by minimum fare" value={baseKm} onChange={setBaseKm} inputMode="decimal" error={fieldErrors.baseKm} />
              <TextField label="Rate per extra km (₹)" value={perKm} onChange={setPerKm} inputMode="decimal" error={fieldErrors.perKm} />
            </>
          ) : (
            <>
              <TextField label="Up to distance (km)" value={upToKm} onChange={setUpToKm} inputMode="decimal" error={fieldErrors.upToKm} />
              <TextField label={mode === "LOCAL_TRAIN" ? "Monthly pass price (₹)" : "Fare per trip (₹)"} value={baseFare} onChange={setBaseFare} inputMode="decimal" error={fieldErrors.baseFare} />
            </>
          )}
        </div>
        <TextField label="Description" value={description} onChange={setDescription} maxLength={200} placeholder="e.g. Second-class monthly season ticket, up to 10 km" error={fieldErrors.description} />
        <TextField label="Source" value={sourceName} onChange={setSourceName} maxLength={200} placeholder="e.g. Central Railway booking counter, Matunga" error={fieldErrors.sourceName} />
        <TextField label="Source link (optional)" value={sourceUrl} onChange={setSourceUrl} maxLength={500} />
        <div>
          <label htmlFor="effective" className="block text-sm font-medium text-slate-800">Applies from</label>
          <input id="effective" type="date" value={effectiveFrom} onChange={(e) => setEffectiveFrom(e.target.value)} className="mt-1 rounded-md border border-slate-300 px-3 py-2" />
          {fieldErrors.effectiveFrom && <p className="mt-1 text-sm text-red-700">{fieldErrors.effectiveFrom}</p>}
        </div>
        <SubmitButton pending={pending}>Add rule</SubmitButton>
      </form>
    </div>
  );
}
