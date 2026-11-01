"use client";

import { useId, type ReactNode } from "react";

// Accessible form building blocks: every input has a <label>, and errors are linked to
// their input with aria-describedby so screen readers announce them.

const inputClass =
  "mt-1 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-slate-900 " +
  "focus:border-teal-600 focus:outline-none focus:ring-2 focus:ring-teal-600/30 " +
  "aria-[invalid=true]:border-red-600 disabled:bg-slate-100 disabled:text-slate-500";

type FieldProps = {
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: "text" | "email" | "password" | "tel";
  error?: string;
  hint?: ReactNode;
  autoComplete?: string;
  required?: boolean;
  maxLength?: number;
  disabled?: boolean;
  /** Shows a numeric keyboard on phones (we keep type="text" so the value is never silently rounded). */
  inputMode?: "numeric" | "decimal";
  placeholder?: string;
};

export function TextField({ label, value, onChange, type = "text", error, hint, ...rest }: FieldProps) {
  const id = useId();
  const describedBy = [hint ? `${id}-hint` : null, error ? `${id}-error` : null].filter(Boolean).join(" ") || undefined;
  return (
    <div>
      <label htmlFor={id} className="block text-sm font-medium text-slate-800">
        {label}
      </label>
      <input
        id={id}
        type={type}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        className={inputClass}
        {...rest}
      />
      {hint && (
        <p id={`${id}-hint`} className="mt-1 text-xs text-slate-500">
          {hint}
        </p>
      )}
      {error && (
        <p id={`${id}-error`} className="mt-1 text-sm text-red-700">
          {error}
        </p>
      )}
    </div>
  );
}

type SelectProps = {
  label: string;
  value: string;
  onChange: (value: string) => void;
  options: { value: string; label: string }[];
  error?: string;
  hint?: ReactNode;
  disabled?: boolean;
};

export function SelectField({ label, value, onChange, options, error, hint, disabled }: SelectProps) {
  const id = useId();
  const describedBy = [hint ? `${id}-hint` : null, error ? `${id}-error` : null].filter(Boolean).join(" ") || undefined;
  return (
    <div>
      <label htmlFor={id} className="block text-sm font-medium text-slate-800">
        {label}
      </label>
      <select
        id={id}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        disabled={disabled}
        className={inputClass}
      >
        {options.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
      {hint && (
        <p id={`${id}-hint`} className="mt-1 text-xs text-slate-500">
          {hint}
        </p>
      )}
      {error && (
        <p id={`${id}-error`} className="mt-1 text-sm text-red-700">
          {error}
        </p>
      )}
    </div>
  );
}

export function SubmitButton({ pending, children }: { pending: boolean; children: ReactNode }) {
  return (
    <button
      type="submit"
      disabled={pending}
      className="inline-flex items-center justify-center rounded-md bg-teal-700 px-4 py-2 font-medium text-white hover:bg-teal-800 focus:outline-none focus:ring-2 focus:ring-teal-600 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60"
    >
      {pending ? "Please wait…" : children}
    </button>
  );
}

/** Message above or below a form: errors in red, confirmations in green. */
export function FormMessage({ kind, children }: { kind: "error" | "success"; children: ReactNode }) {
  return kind === "error" ? (
    <p role="alert" className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
      {children}
    </p>
  ) : (
    <p role="status" className="rounded-md border border-green-200 bg-green-50 px-3 py-2 text-sm text-green-800">
      {children}
    </p>
  );
}
