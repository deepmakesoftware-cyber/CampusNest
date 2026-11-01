"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";
import { FormMessage, SubmitButton, TextField } from "@/components/form";
import { errorMessage } from "@/lib/api";
import { ROLE_HOME, useAuth } from "@/lib/auth";

export default function LoginForm() {
  const { state, login } = useAuth();
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Already logged in? Go straight to the dashboard.
  useEffect(() => {
    if (state.status === "authenticated") {
      router.replace(ROLE_HOME[state.user.role]);
    }
  }, [state, router]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setPending(true);
    setError(null);
    try {
      const user = await login(email, password);
      router.push(ROLE_HOME[user.role]);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="mt-8 space-y-4" noValidate>
      {error && <FormMessage kind="error">{error}</FormMessage>}
      <TextField label="Email" type="email" value={email} onChange={setEmail} autoComplete="email" required />
      <TextField
        label="Password"
        type="password"
        value={password}
        onChange={setPassword}
        autoComplete="current-password"
        required
      />
      <SubmitButton pending={pending}>Log in</SubmitButton>
      <p className="text-sm text-slate-600">
        New to CampusNest?{" "}
        <Link href="/register" className="text-teal-700 underline underline-offset-4">
          Create an account
        </Link>
      </p>
    </form>
  );
}
