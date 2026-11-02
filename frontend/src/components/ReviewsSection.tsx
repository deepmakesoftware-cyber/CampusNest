"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import ReportButton from "@/components/ReportButton";
import { api, errorMessage } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { formatDate } from "@/lib/listingLabels";
import type { ListingReviews, MyReview } from "@/lib/types";

function Stars({ value }: { value: number }) {
  return (
    <span aria-label={`${value} out of 5 stars`} className="text-amber-500">
      {"★".repeat(value)}<span className="text-slate-300">{"★".repeat(5 - value)}</span>
    </span>
  );
}

/** Reviews of one listing. Ratings are opinions from students who contacted the owner, not proof the listing is genuine. */
export default function ReviewsSection({ listingId }: { listingId: number }) {
  const { state } = useAuth();
  const isStudent = state.status === "authenticated" && state.user.role === "STUDENT";
  const loggedIn = state.status === "authenticated";
  const [data, setData] = useState<ListingReviews | null>(null);
  const [mine, setMine] = useState<MyReview | null>(null);
  const [rating, setRating] = useState(0);
  const [comment, setComment] = useState("");
  const [message, setMessage] = useState<{ kind: "error" | "success"; text: string } | null>(null);

  const load = useCallback(() => {
    api<ListingReviews>(`/api/v1/listings/${listingId}/reviews`).then(setData).catch(() => setData(null));
    if (isStudent) {
      api<MyReview>(`/api/v1/student/listings/${listingId}/review`)
        .then((m) => {
          setMine(m);
          if (m.review) {
            setRating(m.review.rating);
            setComment(m.review.comment ?? "");
          }
        })
        .catch(() => setMine(null));
    }
  }, [listingId, isStudent]);

  useEffect(load, [load]);

  async function save(event: FormEvent) {
    event.preventDefault();
    setMessage(null);
    try {
      await api(`/api/v1/student/listings/${listingId}/review`, { method: "PUT", json: { rating, comment } });
      setMessage({ kind: "success", text: "Thank you, your review is saved." });
      load();
    } catch (e) {
      setMessage({ kind: "error", text: errorMessage(e) });
    }
  }

  return (
    <section className="rounded-lg border border-slate-200 bg-white p-5">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h2 className="font-semibold text-slate-900">Student reviews</h2>
        {data && data.count > 0 && (
          <span className="text-sm text-slate-700"><Stars value={Math.round(data.average ?? 0)} /> {data.average} from {data.count} review{data.count === 1 ? "" : "s"}</span>
        )}
      </div>
      <p className="mt-1 text-xs text-slate-500">
        Written by students who contacted the owner through CampusNest. We cannot confirm they stayed there, so treat reviews as opinions.
      </p>

      {data && data.count === 0 && <p className="mt-3 text-sm text-slate-600">No reviews yet.</p>}
      {data && data.count > 0 && (
        <ul className="mt-3 space-y-3">
          {data.reviews.map((r) => (
            <li key={r.id} className="border-t border-slate-100 pt-3 text-sm">
              <div className="flex flex-wrap items-center gap-2">
                <Stars value={r.rating} />
                <span className="font-medium text-slate-800">{r.reviewerName}</span>
                <span className="text-xs text-slate-500">{formatDate(r.createdAt)} · stay not verified</span>
              </div>
              {r.comment && <p className="mt-1 whitespace-pre-line text-slate-700">{r.comment}</p>}
              {loggedIn && <ReportButton targetType="REVIEW" targetId={r.id} label="Report review" />}
            </li>
          ))}
        </ul>
      )}

      {isStudent && mine && (
        mine.eligible ? (
          <form onSubmit={save} className="mt-4 space-y-2 border-t border-slate-100 pt-4">
            <fieldset>
              <legend className="text-sm font-medium text-slate-800">{mine.review ? "Your review" : "Write a review"}</legend>
              <div className="mt-1 flex gap-1">
                {[1, 2, 3, 4, 5].map((n) => (
                  <label key={n} className="cursor-pointer text-2xl">
                    <input type="radio" name="rating" checked={rating === n} onChange={() => setRating(n)} className="sr-only" />
                    <span aria-label={`${n} star${n === 1 ? "" : "s"}`} className={n <= rating ? "text-amber-500" : "text-slate-300"}>★</span>
                  </label>
                ))}
              </div>
            </fieldset>
            <textarea value={comment} onChange={(e) => setComment(e.target.value)} maxLength={1000} rows={3} placeholder="What was it like? Please don't include phone numbers." className="block w-full rounded-md border border-slate-300 px-3 py-2 text-sm" />
            {mine.review && !mine.review.visible && <p className="text-xs text-amber-800">An administrator hid your review; it is not shown to others.</p>}
            {message && <p className={`text-sm ${message.kind === "error" ? "text-red-700" : "text-green-800"}`}>{message.text}</p>}
            <button type="submit" disabled={rating === 0} className="rounded-md bg-teal-700 px-3 py-1.5 text-sm font-medium text-white disabled:opacity-50">
              {mine.review ? "Update review" : "Post review"}
            </button>
          </form>
        ) : (
          <p className="mt-4 border-t border-slate-100 pt-4 text-sm text-slate-600">{mine.reason}</p>
        )
      )}
    </section>
  );
}
