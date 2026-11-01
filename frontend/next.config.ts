import type { NextConfig } from "next";

// Where the Spring Boot API runs. Set BACKEND_URL in frontend/.env.local to change it.
const backendUrl = process.env.BACKEND_URL ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  /* config options here */
  cacheComponents: true,
  partialPrefetching: true,
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
  // Forward every /api/* request to Spring Boot. The browser only ever talks to
  // this Next.js server (one origin), so session cookies work without CORS.
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: `${backendUrl}/api/:path*`,
      },
    ];
  },
};

export default nextConfig;
