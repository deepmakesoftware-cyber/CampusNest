"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { FormMessage, SubmitButton, TextField } from "@/components/form";
import { ApiError, errorMessage } from "@/lib/api";
import { ROLE_HOME, useAuth, type RegisterInput } from "@/lib/auth";

const ROLE_CHOICES: { value: RegisterInput["role"]; title: string; description: string }[] = [
  { value: "STUDENT", title: "I'm a student", description: "Looking for accommodation or a roommate" },
  { value: "OWNER", title: "I'm an accommodation provider", description: "Owner, PG or hostel operator listing rooms" },
];

export default function RegisterForm() {
  const { register } = useAuth();
  const router = useRouter();
  const [role, setRole] = useState<RegisterInput["role"]>("STUDENT");
  const [displayName, setDisplayName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setPending(true);
    setError(null);
    setFieldErrors({});
    try {
      const user = await register({ role, displayName, email, password });
      router.push(ROLE_HOME[user.role]);
    } catch (e) {
      setError(errorMessage(e));
      if (e instanceof ApiError) setFieldErrors(e.fields);
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="mt-8 space-y-5" noValidate>
      {error && <FormMessage kind="error">{error}</FormMessage>}

      <fieldset>
        <legend className="text-sm font-medium text-slate-800">Account type</legend>
        <div className="mt-2 grid gap-3 sm:grid-cols-2">
          {ROLE_CHOICES.map((choice) => (
            <label
              key={choice.value}
              className={`cursor-pointer rounded-lg border p-3 ${
                role === choice.value ? "border-teal-600 bg-teal-50 ring-1 ring-teal-600" : "border-slate-300 bg-white"
              }`}
            >
              <input
                type="radio"
                name="role"
                value={choice.value}
                checked={role === choice.value}
                onChange={() => setRole(choice.value)}
                className="sr-only"
              />
              <span className="block font-medium text-slate-900">{choice.title}</span>
              <span className="mt-0.5 block text-sm text-slate-600">{choice.description}</span>
            </label>
          ))}
        </div>
      </fieldset>

      <TextField
        label="Full name"
        value={displayName}
        onChange={setDisplayName}
        autoComplete="name"
        maxLength={60}
        error={fieldErrors.displayName}
        required
      />
      <TextField
        label="Email"
        type="email"
        value={email}
        onChange={setEmail}
        autoComplete="email"
        error={fieldErrors.email}
        required
      />
      <TextField
        label="Password"
        type="password"
        value={password}
        onChange={setPassword}
        autoComplete="new-password"
        maxLength={72}
        hint="At least 8 characters."
        error={fieldErrors.password}
        required
      />

      <SubmitButton pending={pending}>Create account</SubmitButton>
      <p className="text-sm text-slate-600">
        Already registered?{" "}
        <Link href="/login" className="text-teal-700 underline underline-offset-4">
          Log in
        </Link>
      </p>
    </form>
  );
}
