import type { Metadata } from "next";
import RoleGate from "@/components/RoleGate";
import AdminListingQueue from "./AdminListingQueue";

export const metadata: Metadata = {
  title: "Listing review · CampusNest admin",
};

export default function AdminListingsPage() {
  return (
    <main className="mx-auto w-full max-w-5xl px-4 py-10">
      <RoleGate allow={["ADMIN"]}>
        <AdminListingQueue />
      </RoleGate>
    </main>
  );
}
