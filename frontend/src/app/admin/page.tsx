import type { Metadata } from "next";
import RoleGate from "@/components/RoleGate";
import AdminUsers from "./AdminUsers";

export const metadata: Metadata = {
  title: "Users · CampusNest admin",
};

export default function AdminPage() {
  return (
    <main className="mx-auto w-full max-w-5xl px-4 py-10">
      <RoleGate allow={["ADMIN"]}>
        <AdminUsers />
      </RoleGate>
    </main>
  );
}
