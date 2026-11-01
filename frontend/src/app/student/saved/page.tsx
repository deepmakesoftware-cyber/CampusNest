import type { Metadata } from "next";
import RoleGate from "@/components/RoleGate";
import SavedListings from "./SavedListings";

export const metadata: Metadata = {
  title: "Saved listings · CampusNest",
};

export default function SavedPage() {
  return (
    <main className="mx-auto w-full max-w-3xl px-4 py-10">
      <RoleGate allow={["STUDENT"]}>
        <SavedListings />
      </RoleGate>
    </main>
  );
}
