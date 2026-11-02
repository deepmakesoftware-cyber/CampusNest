import type { Metadata } from "next";
import RoleGate from "@/components/RoleGate";
import RoommateHub from "./RoommateHub";

export const metadata: Metadata = {
  title: "Roommates · CampusNest",
};

export default function RoommatesPage() {
  return (
    <main className="mx-auto w-full max-w-4xl px-4 py-10">
      <RoleGate allow={["STUDENT"]}>
        <RoommateHub />
      </RoleGate>
    </main>
  );
}
