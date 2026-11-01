import type { Metadata } from "next";
import RoleGate from "@/components/RoleGate";
import OwnerHome from "./OwnerHome";

export const metadata: Metadata = {
  title: "Provider dashboard · CampusNest",
};

export default function OwnerPage() {
  return (
    <main className="mx-auto w-full max-w-3xl px-4 py-10">
      <RoleGate allow={["OWNER"]}>
        <OwnerHome />
      </RoleGate>
    </main>
  );
}
