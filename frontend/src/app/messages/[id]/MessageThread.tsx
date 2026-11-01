"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";
import { FormMessage } from "@/components/form";
import { api, errorMessage } from "@/lib/api";
import { formatDateTime } from "@/lib/listingLabels";
import type { EnquiryView } from "@/lib/types";

export default function MessageThread() {
  const { id } = useParams<{ id: string }>();
  const [thread, setThread] = useState<EnquiryView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState("");
  const [share, setShare] = useState(false);
  const [pending, setPending] = useState(false);

  useEffect(() => {
    api<EnquiryView>(`/api/v1/enquiries/${id}`)
      .then(setThread)
      .catch((e) => setError(errorMessage(e)));
  }, [id]);

  async function send(event: FormEvent) {
    event.preventDefault();
    setPending(true);
    setError(null);
    try {
      setThread(await api<EnquiryView>(`/api/v1/enquiries/${id}/messages`, { method: "POST", json: { message, shareContact: share } }));
      setMessage("");
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setPending(false);
    }
  }

  if (error && !thread) return <FormMessage kind="error">{error}</FormMessage>;
  if (!thread) return <p className="text-slate-600">Loading…</p>;

  const otherLabel = thread.myRole === "STUDENT" ? "owner" : "student";

  return (
    <div className="space-y-4">
      <Link href="/messages" className="text-sm text-teal-700 underline underline-offset-4">← Messages</Link>
      <div>
        <h1 className="text-xl font-semibold text-slate-900">{thread.otherName}</h1>
        <p className="text-sm text-slate-600">
          About <Link href={`/listings/${thread.listingId}`} className="underline underline-offset-4">{thread.listingTitle}</Link>
          {thread.listingLocality && `, ${thread.listingLocality}`}
        </p>
      </div>

      <div className="rounded-md border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700">
        {thread.otherEmail || thread.otherPhone ? (
          <>The {otherLabel} shared their contact details: {[thread.otherEmail, thread.otherPhone].filter(Boolean).join(" · ")}</>
        ) : (
          <>The {otherLabel} has not shared contact details. You can talk here.</>
        )}
        <span className="block text-xs text-slate-500">
          {thread.iShareContact ? "You have shared your contact details in this conversation." : "Your email and phone are hidden from them."}
        </span>
      </div>

      <ol className="space-y-3" aria-label="Messages">
        {thread.messages.map((m) => (
          <li key={m.id} className={`flex ${m.fromMe ? "justify-end" : "justify-start"}`}>
            <div className={`max-w-[85%] rounded-lg px-3 py-2 ${m.fromMe ? "bg-teal-700 text-white" : "border border-slate-200 bg-white text-slate-800"}`}>
              {/* Message text is rendered as plain text by React, so it can never run as HTML. */}
              <p className="whitespace-pre-line break-words">{m.body}</p>
              <p className={`mt-1 text-xs ${m.fromMe ? "text-teal-100" : "text-slate-500"}`}>
                {m.fromMe ? "You" : m.senderName} · {formatDateTime(m.at)}
              </p>
            </div>
          </li>
        ))}
      </ol>

      <form onSubmit={send} className="space-y-2 rounded-lg border border-slate-200 bg-white p-4">
        <label htmlFor="reply" className="text-sm font-medium text-slate-800">Reply</label>
        <textarea id="reply" value={message} onChange={(e) => setMessage(e.target.value)} rows={3} maxLength={2000} className="block w-full rounded-md border border-slate-300 px-3 py-2" />
        {!thread.iShareContact && (
          <label className="flex items-center gap-2 text-sm text-slate-700">
            <input type="checkbox" checked={share} onChange={(e) => setShare(e.target.checked)} />
            Also share my email and phone with this {otherLabel}
          </label>
        )}
        {error && <FormMessage kind="error">{error}</FormMessage>}
        <button type="submit" disabled={pending || !message.trim()} className="rounded-md bg-teal-700 px-4 py-2 font-medium text-white disabled:opacity-50">
          {pending ? "Sending…" : "Send"}
        </button>
      </form>
    </div>
  );
}
