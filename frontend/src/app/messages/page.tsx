import type { Metadata } from "next";
import RoleGate from "@/components/RoleGate";
import MessageInbox from "./MessageInbox";

export const metadata: Metadata = {
  title: "Messages · CampusNest",
};

export default function MessagesPage() {
  return (
    <main className="mx-auto w-full max-w-3xl px-4 py-10">
      <RoleGate allow={["STUDENT", "OWNER"]}>
        <MessageInbox />
      </RoleGate>
    </main>
  );
}
