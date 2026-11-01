import type { Metadata } from "next";
import RegisterForm from "./RegisterForm";

export const metadata: Metadata = {
  title: "Create account · CampusNest",
};

export default function RegisterPage() {
  return (
    <main className="mx-auto w-full max-w-md px-4 py-12">
      <h1 className="text-2xl font-semibold text-slate-900">Create your account</h1>
      <p className="mt-1 text-slate-600">Free for students and accommodation providers.</p>
      <RegisterForm />
    </main>
  );
}
