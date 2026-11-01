import type { Metadata } from "next";
import RoleGate from "@/components/RoleGate";
import FareRulesAdmin from "./FareRulesAdmin";

export const metadata: Metadata = {
  title: "Fare rules · CampusNest admin",
};

export default function FaresPage() {
  return (
    <main className="mx-auto w-full max-w-4xl px-4 py-10">
      <RoleGate allow={["ADMIN"]}>
        <FareRulesAdmin />
      </RoleGate>
    </main>
  );
}
