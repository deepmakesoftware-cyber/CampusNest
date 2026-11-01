import type { Metadata } from "next";
import RoleGate from "@/components/RoleGate";
import AccountForms from "./AccountForms";

export const metadata: Metadata = {
  title: "Account · CampusNest",
};

export default function AccountPage() {
  return (
    <main className="mx-auto w-full max-w-2xl px-4 py-10">
      <RoleGate allow={["STUDENT", "OWNER", "ADMIN"]}>
        <h1 className="text-2xl font-semibold text-slate-900">Your account</h1>
        <AccountForms />
      </RoleGate>
    </main>
  );
}
