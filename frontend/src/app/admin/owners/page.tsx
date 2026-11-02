import type { Metadata } from "next";
import RoleGate from "@/components/RoleGate";
import OwnerVerificationQueue from "./OwnerVerificationQueue";

export const metadata: Metadata = {
  title: "Owner verification · CampusNest admin",
};

export default function OwnersPage() {
  return (
    <main className="mx-auto w-full max-w-5xl px-4 py-10">
      <RoleGate allow={["ADMIN"]}>
        <OwnerVerificationQueue />
      </RoleGate>
    </main>
  );
}
