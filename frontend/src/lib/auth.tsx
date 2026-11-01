"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { api } from "./api";
import type { CurrentUser, Role } from "./types";

// Who is logged in, shared with every component through React context.
// This is for display only (which links to show). The BACKEND decides what each user may do.

export type AuthState =
  | { status: "loading" }
  | { status: "anonymous" }
  | { status: "authenticated"; user: CurrentUser };

export type RegisterInput = {
  email: string;
  password: string;
  displayName: string;
  role: "STUDENT" | "OWNER";
};

type AuthContextValue = {
  state: AuthState;
  login: (email: string, password: string) => Promise<CurrentUser>;
  register: (input: RegisterInput) => Promise<CurrentUser>;
  logout: () => Promise<void>;
  /** Call after the user edits their own account details. */
  setUser: (user: CurrentUser) => void;
};

/** Where each role lands after logging in. */
export const ROLE_HOME: Record<Role, string> = {
  STUDENT: "/student",
  OWNER: "/owner",
  ADMIN: "/admin",
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>({ status: "loading" });

  // On page load, ask the backend whether our session cookie belongs to a logged-in user.
  useEffect(() => {
    api<CurrentUser>("/api/v1/auth/me")
      .then((user) => setState({ status: "authenticated", user }))
      .catch(() => setState({ status: "anonymous" }));
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    // Form-encoded, because the backend uses Spring Security's standard login filter.
    const user = await api<CurrentUser>("/api/v1/auth/login", { method: "POST", form: { email, password } });
    setState({ status: "authenticated", user });
    return user;
  }, []);

  const register = useCallback(
    async (input: RegisterInput) => {
      await api<CurrentUser>("/api/v1/auth/register", { method: "POST", json: input });
      return login(input.email, input.password);
    },
    [login],
  );

  const logout = useCallback(async () => {
    try {
      await api<void>("/api/v1/auth/logout", { method: "POST" });
    } finally {
      setState({ status: "anonymous" });
    }
  }, []);

  const setUser = useCallback((user: CurrentUser) => setState({ status: "authenticated", user }), []);

  const value = useMemo(() => ({ state, login, register, logout, setUser }), [state, login, register, logout, setUser]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used inside <AuthProvider>");
  }
  return ctx;
}
