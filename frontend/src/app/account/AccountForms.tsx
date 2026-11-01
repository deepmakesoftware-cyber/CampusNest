"use client";

import { useEffect, useState, type FormEvent, type ReactNode } from "react";
import { FormMessage, SelectField, SubmitButton, TextField } from "@/components/form";
import { ApiError, api, errorMessage } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import type { College, CurrentUser, Gender, OwnerProfile, OwnerType, StudentProfile } from "@/lib/types";

export default function AccountForms() {
  const { state } = useAuth();
  if (state.status !== "authenticated") return null;
  const { user } = state;
  return (
    <div className="mt-6 space-y-8">
      <AccountDetailsForm user={user} />
      {user.role === "STUDENT" && <StudentProfileForm />}
      {user.role === "OWNER" && <OwnerProfileForm />}
    </div>
  );
}

function Section({ title, description, children }: { title: string; description?: string; children: ReactNode }) {
  return (
    <section className="rounded-lg border border-slate-200 bg-white p-5 sm:p-6">
      <h2 className="text-lg font-semibold text-slate-900">{title}</h2>
      {description && <p className="mt-1 text-sm text-slate-600">{description}</p>}
      <div className="mt-5">{children}</div>
    </section>
  );
}

/** Small helper for the save / pending / message cycle shared by all three forms. */
function useSaveState() {
  const [pending, setPending] = useState(false);
  const [message, setMessage] = useState<{ kind: "error" | "success"; text: string } | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  async function run(action: () => Promise<void>) {
    setPending(true);
    setMessage(null);
    setFieldErrors({});
    try {
      await action();
      setMessage({ kind: "success", text: "Saved." });
    } catch (e) {
      setMessage({ kind: "error", text: errorMessage(e) });
      if (e instanceof ApiError) setFieldErrors(e.fields);
    } finally {
      setPending(false);
    }
  }
  return { pending, message, fieldErrors, run };
}

function AccountDetailsForm({ user }: { user: CurrentUser }) {
  const { setUser } = useAuth();
  const [displayName, setDisplayName] = useState(user.displayName);
  const [phone, setPhone] = useState(user.phone ?? "");
  const save = useSaveState();

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    save.run(async () => {
      setUser(await api<CurrentUser>("/api/v1/users/me", { method: "PUT", json: { displayName, phone } }));
    });
  }

  return (
    <Section title="Account details">
      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        <TextField label="Email" value={user.email} onChange={() => {}} disabled hint="Email cannot be changed." />
        <TextField label="Full name" value={displayName} onChange={setDisplayName} maxLength={60} error={save.fieldErrors.displayName} />
        <TextField
          label="Phone (optional)"
          type="tel"
          value={phone}
          onChange={setPhone}
          autoComplete="tel"
          hint="Never shown publicly. Shared only where you choose to share it, e.g. in an enquiry."
          error={save.fieldErrors.phone}
        />
        {save.message && <FormMessage kind={save.message.kind}>{save.message.text}</FormMessage>}
        <SubmitButton pending={save.pending}>Save details</SubmitButton>
      </form>
    </Section>
  );
}

const GENDER_OPTIONS = [
  { value: "", label: "Prefer not to say" },
  { value: "FEMALE", label: "Female" },
  { value: "MALE", label: "Male" },
  { value: "OTHER", label: "Other" },
];

const YEAR_OPTIONS = [{ value: "", label: "Not specified" }].concat(
  [1, 2, 3, 4, 5, 6].map((y) => ({ value: String(y), label: `Year ${y}` })),
);

function StudentProfileForm() {
  const [colleges, setColleges] = useState<College[]>([]);
  const [collegeId, setCollegeId] = useState("");
  const [course, setCourse] = useState("");
  const [year, setYear] = useState("");
  const [gender, setGender] = useState("");
  const [loadError, setLoadError] = useState<string | null>(null);
  const save = useSaveState();

  useEffect(() => {
    Promise.all([api<College[]>("/api/v1/colleges"), api<StudentProfile>("/api/v1/student/profile")])
      .then(([collegeList, profile]) => {
        setColleges(collegeList);
        setCollegeId(profile.collegeId ? String(profile.collegeId) : "");
        setCourse(profile.course ?? "");
        setYear(profile.yearOfStudy ? String(profile.yearOfStudy) : "");
        setGender(profile.gender ?? "");
      })
      .catch((e) => setLoadError(errorMessage(e)));
  }, []);

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    save.run(async () => {
      await api<StudentProfile>("/api/v1/student/profile", {
        method: "PUT",
        json: {
          collegeId: collegeId ? Number(collegeId) : null,
          course: course || null,
          yearOfStudy: year ? Number(year) : null,
          gender: (gender || null) as Gender | null,
        },
      });
    });
  }

  return (
    <Section title="Student profile" description="Used to estimate your commute and to rank accommodation for you.">
      {loadError ? (
        <FormMessage kind="error">{loadError}</FormMessage>
      ) : (
        <form onSubmit={handleSubmit} className="space-y-4" noValidate>
          <SelectField
            label="College"
            value={collegeId}
            onChange={setCollegeId}
            options={[{ value: "", label: "Select your college" }].concat(
              colleges.map((c) => ({ value: String(c.id), label: `${c.name} (${c.shortName})` })),
            )}
          />
          <TextField label="Course" value={course} onChange={setCourse} maxLength={80} hint="For example: MCA, B.Tech Civil" error={save.fieldErrors.course} />
          <SelectField label="Year of study" value={year} onChange={setYear} options={YEAR_OPTIONS} error={save.fieldErrors.yearOfStudy} />
          <SelectField
            label="Gender (optional)"
            value={gender}
            onChange={setGender}
            options={GENDER_OPTIONS}
            hint="Only used to match gender-specific accommodation and roommate preferences. Never shown to other users."
          />
          {save.message && <FormMessage kind={save.message.kind}>{save.message.text}</FormMessage>}
          <SubmitButton pending={save.pending}>Save profile</SubmitButton>
        </form>
      )}
    </Section>
  );
}

const OWNER_TYPE_OPTIONS: { value: OwnerType; label: string }[] = [
  { value: "INDIVIDUAL", label: "Individual owner" },
  { value: "PG_OPERATOR", label: "PG operator" },
  { value: "HOSTEL_OPERATOR", label: "Hostel operator" },
];

const VERIFICATION_LABELS: Record<OwnerProfile["verificationStatus"], string> = {
  UNVERIFIED: "Not verified",
  PENDING: "Verification pending",
  VERIFIED: "Identity checked by CampusNest",
  REJECTED: "Verification rejected",
};

function OwnerProfileForm() {
  const [profile, setProfile] = useState<OwnerProfile | null>(null);
  const [ownerType, setOwnerType] = useState<OwnerType>("INDIVIDUAL");
  const [businessName, setBusinessName] = useState("");
  const [loadError, setLoadError] = useState<string | null>(null);
  const save = useSaveState();

  useEffect(() => {
    api<OwnerProfile>("/api/v1/owner/profile")
      .then((p) => {
        setProfile(p);
        setOwnerType(p.ownerType);
        setBusinessName(p.businessName ?? "");
      })
      .catch((e) => setLoadError(errorMessage(e)));
  }, []);

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    save.run(async () => {
      setProfile(
        await api<OwnerProfile>("/api/v1/owner/profile", {
          method: "PUT",
          json: { ownerType, businessName: businessName || null },
        }),
      );
    });
  }

  if (loadError) return <FormMessage kind="error">{loadError}</FormMessage>;
  return (
    <Section title="Provider profile">
      {profile && (
        <p className="mb-5 text-sm text-slate-700">
          Verification status:{" "}
          <span className="rounded bg-slate-100 px-1.5 py-0.5 font-medium">{VERIFICATION_LABELS[profile.verificationStatus]}</span>
          <span className="mt-1 block text-xs text-slate-500">
            Only CampusNest administrators can change this. Requesting verification opens in a later release.
          </span>
        </p>
      )}
      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        <SelectField label="Provider type" value={ownerType} onChange={(v) => setOwnerType(v as OwnerType)} options={OWNER_TYPE_OPTIONS} />
        <TextField
          label="Business name (optional)"
          value={businessName}
          onChange={setBusinessName}
          maxLength={120}
          hint="For example, the name of your PG or hostel."
          error={save.fieldErrors.businessName}
        />
        {save.message && <FormMessage kind={save.message.kind}>{save.message.text}</FormMessage>}
        <SubmitButton pending={save.pending}>Save profile</SubmitButton>
      </form>
    </Section>
  );
}
