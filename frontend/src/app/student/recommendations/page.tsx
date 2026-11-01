import type { Metadata } from "next";
import RoleGate from "@/components/RoleGate";
import RecommendationList from "./RecommendationList";

export const metadata: Metadata = {
  title: "Recommended for you · CampusNest",
};

export default function RecommendationsPage() {
  return (
    <main className="mx-auto w-full max-w-4xl px-4 py-10">
      <RoleGate allow={["STUDENT"]}>
        <RecommendationList />
      </RoleGate>
    </main>
  );
}
