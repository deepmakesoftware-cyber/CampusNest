import type { Metadata } from "next";
import RoleGate from "@/components/RoleGate";
import PreferencesForm from "./PreferencesForm";

export const metadata: Metadata = {
  title: "Your preferences · CampusNest",
};

export default function PreferencesPage() {
  return (
    <main className="mx-auto w-full max-w-2xl px-4 py-10">
      <RoleGate allow={["STUDENT"]}>
        <PreferencesForm />
      </RoleGate>
    </main>
  );
}
