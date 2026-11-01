"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import ComingSoon from "@/components/ComingSoon";
import { FormMessage } from "@/components/form";
import { api, errorMessage } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import type { StudentProfile } from "@/lib/types";

export default function StudentHome() {
  const { state } = useAuth();
  const [profile, setProfile] = useState<StudentProfile | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api<StudentProfile>("/api/v1/student/profile")
      .then(setProfile)
      .catch((e) => setError(errorMessage(e)));
  }, []);

  const name = state.status === "authenticated" ? state.user.displayName : "";

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">Hi, {name}</h1>
        <p className="mt-1 text-slate-600">Your CampusNest student dashboard.</p>
      </div>

      {error && <FormMessage kind="error">{error}</FormMessage>}

      {profile && (
        <section className="rounded-lg border border-slate-200 bg-white p-5 sm:p-6">
          <h2 className="text-lg font-semibold text-slate-900">Your college</h2>
          {profile.collegeName ? (
            <p className="mt-2 text-slate-700">
              {profile.collegeName}
              {profile.course && ` · ${profile.course}`}
              {profile.yearOfStudy && ` · Year ${profile.yearOfStudy}`}
            </p>
          ) : (
            <p className="mt-2 text-slate-700">
              You haven&apos;t chosen your college yet. CampusNest needs it to estimate commute times.{" "}
              <Link href="/account" className="text-teal-700 underline underline-offset-4">
                Add your college
              </Link>
            </p>
          )}
        </section>
      )}

      <ComingSoon
        items={[
          { phase: "Phase 4", title: "Search accommodation", description: "browse verified listings near your college" },
          { phase: "Phase 5", title: "Ranked recommendations", description: "suitability scores with a full explanation" },
          { phase: "Phase 6", title: "Cost vs commute", description: "compare rent, travel time and transport cost" },
          { phase: "Phase 7", title: "Roommate matching", description: "find compatible roommates, privately" },
        ]}
      />
    </div>
  );
}
