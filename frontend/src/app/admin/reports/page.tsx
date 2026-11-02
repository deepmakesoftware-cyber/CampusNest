import type { Metadata } from "next";
import RoleGate from "@/components/RoleGate";
import ReportQueue from "./ReportQueue";

export const metadata: Metadata = {
  title: "Reports · CampusNest admin",
};

export default function ReportsPage() {
  return (
    <main className="mx-auto w-full max-w-5xl px-4 py-10">
      <RoleGate allow={["ADMIN"]}>
        <ReportQueue />
      </RoleGate>
    </main>
  );
}
