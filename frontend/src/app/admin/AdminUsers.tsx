"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import { FormMessage } from "@/components/form";
import { api, errorMessage } from "@/lib/api";
import { ROLE_LABELS, type AdminUser, type Page, type Role } from "@/lib/types";

const FILTERS: { value: "" | Role; label: string }[] = [
  { value: "", label: "All" },
  { value: "STUDENT", label: "Students" },
  { value: "OWNER", label: "Providers" },
  { value: "ADMIN", label: "Admins" },
];

function formatDate(iso: string | null) {
  return iso ? new Date(iso).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" }) : "Never";
}

export default function AdminUsers() {
  const [roleFilter, setRoleFilter] = useState<"" | Role>("");
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<AdminUser> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [suspendingId, setSuspendingId] = useState<number | null>(null);

  const load = useCallback(() => {
    const params = new URLSearchParams({ page: String(page) });
    if (roleFilter) params.set("role", roleFilter);
    api<Page<AdminUser>>(`/api/v1/admin/users?${params}`)
      .then((result) => {
        setData(result);
        setError(null);
      })
      .catch((e) => setError(errorMessage(e)));
  }, [page, roleFilter]);

  useEffect(load, [load]);

  function replaceUser(updated: AdminUser) {
    setData((current) =>
      current ? { ...current, items: current.items.map((u) => (u.id === updated.id ? updated : u)) } : current,
    );
  }

  async function reinstate(user: AdminUser) {
    setNotice(null);
    try {
      replaceUser(await api<AdminUser>(`/api/v1/admin/users/${user.id}/reinstate`, { method: "POST" }));
      setNotice(`${user.displayName} has been reinstated.`);
    } catch (e) {
      setError(errorMessage(e));
    }
  }

  return (
    <div>
      <h1 className="text-2xl font-semibold text-slate-900">Users</h1>
      <p className="mt-1 text-slate-600">Review accounts and suspend or reinstate access.</p>

      <div className="mt-6 flex flex-wrap gap-2" role="group" aria-label="Filter by role">
        {FILTERS.map((f) => (
          <button
            key={f.label}
            type="button"
            onClick={() => {
              setRoleFilter(f.value);
              setPage(0);
            }}
            aria-pressed={roleFilter === f.value}
            className={`rounded-full border px-3 py-1 text-sm ${
              roleFilter === f.value ? "border-teal-700 bg-teal-700 text-white" : "border-slate-300 bg-white text-slate-700"
            }`}
          >
            {f.label}
          </button>
        ))}
      </div>

      <div className="mt-4 space-y-3">
        {error && <FormMessage kind="error">{error}</FormMessage>}
        {notice && <FormMessage kind="success">{notice}</FormMessage>}
      </div>

      {data && data.items.length === 0 && <p className="mt-8 text-slate-600">No users match this filter.</p>}

      {data && data.items.length > 0 && (
        <div className="mt-4 overflow-x-auto rounded-lg border border-slate-200 bg-white">
          <table className="min-w-full text-left text-sm">
            <thead className="border-b border-slate-200 bg-slate-50 text-slate-600">
              <tr>
                <th scope="col" className="px-4 py-2 font-medium">Name</th>
                <th scope="col" className="px-4 py-2 font-medium">Role</th>
                <th scope="col" className="px-4 py-2 font-medium">Status</th>
                <th scope="col" className="px-4 py-2 font-medium">Joined</th>
                <th scope="col" className="px-4 py-2 font-medium">Last login</th>
                <th scope="col" className="px-4 py-2 font-medium"><span className="sr-only">Actions</span></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {data.items.map((user) => (
                <UserRow
                  key={user.id}
                  user={user}
                  suspending={suspendingId === user.id}
                  onStartSuspend={() => {
                    setNotice(null);
                    setSuspendingId(user.id);
                  }}
                  onCancelSuspend={() => setSuspendingId(null)}
                  onSuspended={(updated) => {
                    replaceUser(updated);
                    setSuspendingId(null);
                    setNotice(`${updated.displayName} has been suspended.`);
                  }}
                  onReinstate={() => reinstate(user)}
                />
              ))}
            </tbody>
          </table>
        </div>
      )}

      {data && data.totalPages > 1 && (
        <div className="mt-4 flex items-center gap-3 text-sm">
          <button type="button" disabled={page === 0} onClick={() => setPage(page - 1)} className="rounded border border-slate-300 px-3 py-1 disabled:opacity-50">
            Previous
          </button>
          <span className="text-slate-600">
            Page {data.page + 1} of {data.totalPages}
          </span>
          <button type="button" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)} className="rounded border border-slate-300 px-3 py-1 disabled:opacity-50">
            Next
          </button>
        </div>
      )}
    </div>
  );
}

type RowProps = {
  user: AdminUser;
  suspending: boolean;
  onStartSuspend: () => void;
  onCancelSuspend: () => void;
  onSuspended: (user: AdminUser) => void;
  onReinstate: () => void;
};

function UserRow({ user, suspending, onStartSuspend, onCancelSuspend, onSuspended, onReinstate }: RowProps) {
  const [reason, setReason] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSuspend(event: FormEvent) {
    event.preventDefault();
    setPending(true);
    setError(null);
    try {
      onSuspended(await api<AdminUser>(`/api/v1/admin/users/${user.id}/suspend`, { method: "POST", json: { reason } }));
      setReason("");
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setPending(false);
    }
  }

  return (
    <>
      <tr>
        <td className="px-4 py-3">
          <div className="font-medium text-slate-900">{user.displayName}</div>
          <div className="text-slate-500">{user.email}</div>
        </td>
        <td className="px-4 py-3 text-slate-700">{ROLE_LABELS[user.role]}</td>
        <td className="px-4 py-3">
          {user.status === "ACTIVE" ? (
            <span className="rounded bg-green-50 px-1.5 py-0.5 text-green-800">Active</span>
          ) : (
            <span className="rounded bg-red-50 px-1.5 py-0.5 text-red-800" title={user.statusReason ?? undefined}>
              Suspended
            </span>
          )}
        </td>
        <td className="px-4 py-3 text-slate-700">{formatDate(user.createdAt)}</td>
        <td className="px-4 py-3 text-slate-700">{formatDate(user.lastLoginAt)}</td>
        <td className="px-4 py-3 text-right">
          {user.role !== "ADMIN" && user.status === "ACTIVE" && !suspending && (
            <button type="button" onClick={onStartSuspend} className="text-red-700 hover:underline">
              Suspend
            </button>
          )}
          {user.status === "SUSPENDED" && (
            <button type="button" onClick={onReinstate} className="text-teal-700 hover:underline">
              Reinstate
            </button>
          )}
        </td>
      </tr>
      {user.status === "SUSPENDED" && user.statusReason && (
        <tr>
          <td colSpan={6} className="px-4 pb-3 text-xs text-slate-500">
            Reason: {user.statusReason}
          </td>
        </tr>
      )}
      {suspending && (
        <tr>
          <td colSpan={6} className="bg-red-50/50 px-4 py-3">
            <form onSubmit={handleSuspend} className="flex flex-col gap-2 sm:flex-row sm:items-start">
              <label className="flex-1 text-sm">
                <span className="sr-only">Reason for suspending {user.displayName}</span>
                <input
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  maxLength={300}
                  placeholder="Reason (required, recorded)"
                  className="w-full rounded-md border border-slate-300 px-3 py-1.5"
                />
                {error && <span className="mt-1 block text-red-700">{error}</span>}
              </label>
              <div className="flex gap-2">
                <button type="submit" disabled={pending || !reason.trim()} className="rounded-md bg-red-700 px-3 py-1.5 text-white disabled:opacity-50">
                  {pending ? "Suspending…" : "Confirm suspend"}
                </button>
                <button type="button" onClick={onCancelSuspend} className="rounded-md border border-slate-300 px-3 py-1.5">
                  Cancel
                </button>
              </div>
            </form>
          </td>
        </tr>
      )}
    </>
  );
}
