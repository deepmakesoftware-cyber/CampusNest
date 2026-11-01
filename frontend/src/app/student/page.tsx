import type { Metadata } from "next";
import RoleGate from "@/components/RoleGate";
import StudentHome from "./StudentHome";

export const metadata: Metadata = {
  title: "Student dashboard · CampusNest",
};

export default function StudentPage() {
  return (
    <main className="mx-auto w-full max-w-3xl px-4 py-10">
      <RoleGate allow={["STUDENT"]}>
        <StudentHome />
      </RoleGate>
    </main>
  );
}
