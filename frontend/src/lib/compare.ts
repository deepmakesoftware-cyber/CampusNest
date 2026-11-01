"use client";

import { useSyncExternalStore } from "react";

// The student's comparison shortlist (up to 4 listing ids), kept in this browser only.
// This is a convenience, not important data, so losing it (private window, cleared storage) is harmless.

const KEY = "campusnest.compare";
const EVENT = "campusnest-compare-changed";
export const MAX_COMPARE = 4;

const EMPTY: number[] = [];
let cachedRaw: string | null = null;
let cachedIds: number[] = EMPTY;

function read(): number[] {
  let raw: string | null = null;
  try {
    raw = window.localStorage.getItem(KEY);
  } catch {
    return EMPTY;
  }
  if (raw === cachedRaw) return cachedIds; // same array object until it really changes
  cachedRaw = raw;
  try {
    const parsed: unknown = raw ? JSON.parse(raw) : [];
    cachedIds = Array.isArray(parsed) ? parsed.filter((n): n is number => typeof n === "number").slice(0, MAX_COMPARE) : EMPTY;
  } catch {
    cachedIds = EMPTY;
  }
  return cachedIds;
}

function write(ids: number[]) {
  try {
    window.localStorage.setItem(KEY, JSON.stringify(ids));
  } catch {
    // storage unavailable: the shortlist simply won't persist
  }
  window.dispatchEvent(new Event(EVENT));
}

function subscribe(callback: () => void) {
  window.addEventListener(EVENT, callback);
  window.addEventListener("storage", callback); // other tabs
  return () => {
    window.removeEventListener(EVENT, callback);
    window.removeEventListener("storage", callback);
  };
}

export function useCompareSelection() {
  const ids = useSyncExternalStore(subscribe, read, () => EMPTY);
  return {
    ids,
    has: (id: number) => ids.includes(id),
    toggle: (id: number) => {
      const current = read();
      if (current.includes(id)) write(current.filter((x) => x !== id));
      else if (current.length < MAX_COMPARE) write([...current, id]);
    },
    clear: () => write([]),
  };
}
