import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import { Suspense } from "react";
import { CompareBar } from "@/components/CompareControls";
import SiteHeader from "@/components/SiteHeader";
import { AuthProvider } from "@/lib/auth";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "CampusNest",
  description: "Smart student accommodation and roommate matching for Mumbai colleges.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang="en"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body className="min-h-full flex flex-col">
        <AuthProvider>
          <SiteHeader />
          <div className="flex flex-1 flex-col">{children}</div>
          {/* reads the current URL, which Cache Components requires inside <Suspense> */}
          <Suspense fallback={null}>
            <CompareBar />
          </Suspense>
        </AuthProvider>
      </body>
    </html>
  );
}
