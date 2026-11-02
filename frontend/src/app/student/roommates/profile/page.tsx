import type { Metadata } from "next";
import RoleGate from "@/components/RoleGate";
import RoommateProfileForm from "./RoommateProfileForm";

export const metadata: Metadata = {
  title: "Roommate profile · CampusNest",
};

export default function RoommateProfilePage() {
  return (
    <main className="mx-auto w-full max-w-2xl px-4 py-10">
      <RoleGate allow={["STUDENT"]}>
        <RoommateProfileForm />
      </RoleGate>
    </main>
  );
}
