import type { Metadata } from "next";
import { Suspense } from "react";
import RoleGate from "@/components/RoleGate";
import MessageThread from "./MessageThread";

export const metadata: Metadata = {
  title: "Conversation · CampusNest",
};

export default function ThreadPage() {
  return (
    <main className="mx-auto w-full max-w-3xl px-4 py-10">
      <RoleGate allow={["STUDENT", "OWNER"]}>
        <Suspense fallback={<p className="text-slate-600">Loading…</p>}>
          <MessageThread />
        </Suspense>
      </RoleGate>
    </main>
  );
}
