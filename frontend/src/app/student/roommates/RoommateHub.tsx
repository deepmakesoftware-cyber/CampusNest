"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import ReportButton from "@/components/ReportButton";
import { FormMessage } from "@/components/form";
import { ApiError, api, errorMessage } from "@/lib/api";
import { formatDate } from "@/lib/listingLabels";
import { FOOD_LABELS, SHARING_LABELS, SLEEP_LABELS, SMOKING_LABELS, STUDY_LABELS } from "@/lib/roommateLabels";
import type { ConnectionView, MatchView, PersonPreview, RoommateRequestView } from "@/lib/types";

type Tab = "matches" | "requests" | "connections";

export default function RoommateHub() {
  const [tab, setTab] = useState<Tab>("matches");
  const [matches, setMatches] = useState<MatchView[] | null>(null);
  const [requests, setRequests] = useState<RoommateRequestView[]>([]);
  const [connections, setConnections] = useState<ConnectionView[]>([]);
  const [needsProfile, setNeedsProfile] = useState(false);
  const [message, setMessage] = useState<{ kind: "error" | "success"; text: string } | null>(null);

  const load = useCallback(() => {
    api<MatchView[]>("/api/v1/student/roommates/matches")
      .then((m) => {
        setMatches(m);
        setNeedsProfile(false);
      })
      .catch((e) => {
        if (e instanceof ApiError && e.code === "ROOMMATE_PROFILE_REQUIRED") setNeedsProfile(true);
        else setMessage({ kind: "error", text: errorMessage(e) });
      });
    api<RoommateRequestView[]>("/api/v1/student/roommates/requests").then(setRequests).catch(() => setRequests([]));
    api<ConnectionView[]>("/api/v1/student/roommates/connections").then(setConnections).catch(() => setConnections([]));
  }, []);

  useEffect(load, [load]);

  async function act(path: string, success: string, body?: unknown) {
    setMessage(null);
    try {
      await api(path, { method: "POST", json: body });
      setMessage({ kind: "success", text: success });
      load();
    } catch (e) {
      setMessage({ kind: "error", text: errorMessage(e) });
    }
  }

  if (needsProfile) {
    return (
      <div className="rounded-lg border border-dashed border-slate-300 bg-white p-8 text-center">
        <h1 className="text-xl font-semibold text-slate-900">Find a compatible roommate</h1>
        <p className="mt-2 text-slate-600">
          Answer a few questions about your habits. Only students who also opted in can see you, and they see only your
          first name, course and habits until you both agree to connect.
        </p>
        <Link href="/student/roommates/profile" className="mt-4 inline-block rounded-md bg-teal-700 px-4 py-2 font-medium text-white">
          Create my roommate profile
        </Link>
      </div>
    );
  }

  const received = requests.filter((r) => r.direction === "RECEIVED");

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">Roommates</h1>
          <p className="mt-1 text-slate-600">Students whose stated habits fit yours, ranked by compatibility.</p>
        </div>
        <Link href="/student/roommates/profile" className="text-sm text-teal-700 underline underline-offset-4">Edit my roommate profile</Link>
      </div>

      <p className="rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-900">
        Compatibility is an estimate from what each person says about their habits. It cannot guarantee you will get along,
        so talk before you decide, and meet in a public place first.
      </p>

      <div className="flex gap-2" role="tablist" aria-label="Roommate sections">
        {([
          ["matches", `Matches${matches ? ` (${matches.length})` : ""}`],
          ["requests", `Requests${received.length ? ` (${received.length} new)` : ""}`],
          ["connections", `Connections (${connections.length})`],
        ] as [Tab, string][]).map(([t, label]) => (
          <button key={t} type="button" role="tab" aria-selected={tab === t} onClick={() => setTab(t)}
            className={`rounded-full border px-3 py-1 text-sm ${tab === t ? "border-teal-700 bg-teal-700 text-white" : "border-slate-300 bg-white text-slate-700"}`}>
            {label}
          </button>
        ))}
      </div>

      {message && <FormMessage kind={message.kind}>{message.text}</FormMessage>}

      {tab === "matches" && (
        matches === null ? <p className="text-slate-600">Finding matches…</p> :
        matches.length === 0 ? (
          <p className="rounded-lg border border-dashed border-slate-300 bg-white p-6 text-center text-slate-600">
            No compatible students yet. Check back as more students join.
          </p>
        ) : (
          <ul className="space-y-4">
            {matches.map((m) => (
              <MatchCard key={m.person.userId} match={m}
                onConnect={(text) => act("/api/v1/student/roommates/requests", "Request sent.", { toUserId: m.person.userId, message: text })}
                onBlock={() => act(`/api/v1/student/roommates/blocks/${m.person.userId}`, "Blocked. You will no longer see each other.")} />
            ))}
          </ul>
        )
      )}

      {tab === "requests" && (
        requests.length === 0 ? <p className="text-slate-600">No pending requests.</p> : (
          <ul className="space-y-3">
            {requests.map((r) => (
              <li key={r.id} className="rounded-lg border border-slate-200 bg-white p-4 text-sm">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span className="font-medium text-slate-900">
                    {r.direction === "RECEIVED" ? `${r.person?.shortName ?? "A student"} wants to connect` : `You asked ${r.person?.shortName ?? "a student"} to connect`}
                  </span>
                  <span className="text-xs text-slate-500">{formatDate(r.createdAt)}</span>
                </div>
                {r.message && <p className="mt-1 text-slate-700">“{r.message}”</p>}
                <div className="mt-3 flex gap-2">
                  {r.direction === "RECEIVED" ? (
                    <>
                      <button type="button" onClick={() => act(`/api/v1/student/roommates/requests/${r.id}/accept`, "Connected. You can now see each other's contact details.")} className="rounded-md bg-teal-700 px-3 py-1.5 font-medium text-white">Accept</button>
                      <button type="button" onClick={() => act(`/api/v1/student/roommates/requests/${r.id}/decline`, "Request declined.")} className="rounded-md border border-slate-300 px-3 py-1.5">Decline</button>
                    </>
                  ) : (
                    <button type="button" onClick={() => act(`/api/v1/student/roommates/requests/${r.id}/withdraw`, "Request withdrawn.")} className="rounded-md border border-slate-300 px-3 py-1.5">Withdraw</button>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )
      )}

      {tab === "connections" && (
        connections.length === 0 ? <p className="text-slate-600">No connections yet. When both of you accept, contact details appear here.</p> : (
          <ul className="space-y-3">
            {connections.map((c) => (
              <li key={c.requestId} className="rounded-lg border border-slate-200 bg-white p-4 text-sm">
                <div className="font-medium text-slate-900">{c.displayName}</div>
                <div className="text-slate-600">{[c.course, c.college].filter(Boolean).join(" · ")}</div>
                <div className="mt-2 text-slate-800">{c.email}{c.phone && ` · ${c.phone}`}</div>
                <div className="text-xs text-slate-500">Connected {formatDate(c.connectedAt)}</div>
              </li>
            ))}
          </ul>
        )
      )}
    </div>
  );
}

function MatchCard({ match, onConnect, onBlock }: { match: MatchView; onConnect: (message: string) => void; onBlock: () => void }) {
  const { person: p, compatibility: c } = match;
  const [open, setOpen] = useState(false);
  const [writing, setWriting] = useState(false);
  const [text, setText] = useState("");

  return (
    <li className="rounded-lg border border-slate-200 bg-white p-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <div className="font-semibold text-slate-900">{p.shortName}</div>
          <div className="text-sm text-slate-600">{[p.course, p.yearOfStudy && `Year ${p.yearOfStudy}`, p.college].filter(Boolean).join(" · ")}</div>
          {p.about && <p className="mt-1 text-sm text-slate-700">{p.about}</p>}
        </div>
        <div className="text-right">
          <div className="text-2xl font-bold text-teal-800">{c.percent}%</div>
          <div className="text-xs text-slate-500">compatibility · {c.factorsUsed} of {c.factorsTotal} factors</div>
        </div>
      </div>
      <p className="mt-2 text-sm text-slate-800">{c.summary}</p>

      <div className="mt-3 grid gap-3 text-sm sm:grid-cols-2">
        <div>
          <div className="font-medium text-green-800">In common</div>
          <ul className="mt-1 list-disc pl-5 text-slate-700">{c.matches.map((m) => <li key={m}>{m}</li>)}</ul>
        </div>
        <div>
          <div className="font-medium text-amber-800">Differences to talk about</div>
          {c.differences.length === 0 ? <p className="mt-1 text-slate-500">None found.</p> : (
            <ul className="mt-1 list-disc pl-5 text-slate-700">{c.differences.map((d) => <li key={d}>{d}</li>)}</ul>
          )}
        </div>
      </div>

      <button type="button" onClick={() => setOpen(!open)} aria-expanded={open} className="mt-3 text-sm text-teal-700 underline underline-offset-4">
        {open ? "Hide details" : "Their stated habits and the score breakdown"}
      </button>
      {open && <Details person={p} factors={c.factors} />}

      <div className="mt-4 flex flex-wrap items-center gap-2 border-t border-slate-100 pt-3 text-sm">
        {match.relationship === "NONE" && !writing && (
          <button type="button" onClick={() => setWriting(true)} className="rounded-md bg-teal-700 px-3 py-1.5 font-medium text-white">Ask to connect</button>
        )}
        {match.relationship === "REQUEST_SENT" && <span className="text-slate-600">Request sent: waiting for their answer.</span>}
        {match.relationship === "REQUEST_RECEIVED" && <span className="text-slate-600">They asked to connect: see the Requests tab.</span>}
        {match.relationship === "CONNECTED" && <span className="text-green-800">Connected: see the Connections tab.</span>}
        <span className="ml-auto flex items-center gap-3">
          <ReportButton targetType="USER" targetId={p.userId} />
          <button type="button" onClick={onBlock} className="text-xs text-slate-500 underline underline-offset-4">Block</button>
        </span>
      </div>
      {writing && (
        <form onSubmit={(e) => { e.preventDefault(); onConnect(text); setWriting(false); }} className="mt-3 space-y-2">
          <label htmlFor={`msg-${p.userId}`} className="text-sm text-slate-700">A short note (optional). Don&apos;t include contact details yet.</label>
          <textarea id={`msg-${p.userId}`} value={text} onChange={(e) => setText(e.target.value)} maxLength={500} rows={2} className="block w-full rounded-md border border-slate-300 px-3 py-2 text-sm" />
          <div className="flex gap-2">
            <button type="submit" className="rounded-md bg-teal-700 px-3 py-1.5 text-sm font-medium text-white">Send request</button>
            <button type="button" onClick={() => setWriting(false)} className="rounded-md border border-slate-300 px-3 py-1.5 text-sm">Cancel</button>
          </div>
        </form>
      )}
    </li>
  );
}

function Details({ person: p, factors }: { person: PersonPreview; factors: MatchView["compatibility"]["factors"] }) {
  return (
    <div className="mt-3 grid gap-4 rounded-md bg-slate-50 p-3 text-sm sm:grid-cols-2">
      <dl className="space-y-1 text-slate-700">
        <div><dt className="inline text-slate-500">Sleep: </dt><dd className="inline">{SLEEP_LABELS[p.sleepSchedule]}</dd></div>
        <div><dt className="inline text-slate-500">Study: </dt><dd className="inline">{STUDY_LABELS[p.studyEnvironment]}</dd></div>
        <div><dt className="inline text-slate-500">Cleanliness: </dt><dd className="inline">{p.cleanliness}/5</dd></div>
        <div><dt className="inline text-slate-500">Guests: </dt><dd className="inline">{p.guests}/5</dd></div>
        <div><dt className="inline text-slate-500">Smoking: </dt><dd className="inline">{SMOKING_LABELS[p.smoking]}</dd></div>
        {p.foodPreference && <div><dt className="inline text-slate-500">Food: </dt><dd className="inline">{FOOD_LABELS[p.foodPreference]}</dd></div>}
        <div><dt className="inline text-slate-500">Wants: </dt><dd className="inline">{SHARING_LABELS[p.sharingSize]}</dd></div>
        <div><dt className="inline text-slate-500">Areas: </dt><dd className="inline">{p.anywhereNearCollege ? "Anywhere near college" : p.preferredLocalities.join(", ")}</dd></div>
      </dl>
      <ul className="space-y-1">
        {factors.map((f) => (
          <li key={f.key} className="flex justify-between gap-2 text-slate-700">
            <span>{f.label} <span className="text-slate-400">({f.weight})</span></span>
            <span className="tabular-nums">{f.score}/100</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
