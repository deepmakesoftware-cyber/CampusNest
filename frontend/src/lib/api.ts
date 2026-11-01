// Small wrapper around fetch() for calling the CampusNest API from the browser.
//
// - Relative URLs ("/api/v1/...") go to Next.js, which forwards them to Spring Boot.
// - The session cookie is sent automatically (same origin). JavaScript never sees it (HttpOnly).
// - For POST/PUT/PATCH/DELETE we add the CSRF token: Spring puts it in the readable XSRF-TOKEN
//   cookie and expects it back in the X-XSRF-TOKEN header. A malicious site cannot read our
//   cookies, so it cannot forge this header.

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
    public readonly fields: Record<string, string> = {},
  ) {
    super(message);
  }
}

type ApiErrorBody = { error?: string; message?: string; fields?: Record<string, string> };

type Method = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

type RequestOptions = {
  method?: Method;
  /** Sent as a JSON body. */
  json?: unknown;
  /** Sent as a form-encoded body (used by the login endpoint). */
  form?: Record<string, string>;
};

const CSRF_COOKIE = "XSRF-TOKEN";
const CSRF_HEADER = "X-XSRF-TOKEN";

function readCookie(name: string): string | null {
  const entry = document.cookie.split("; ").find((c) => c.startsWith(`${name}=`));
  return entry ? decodeURIComponent(entry.slice(name.length + 1)) : null;
}

/** Returns the CSRF token, asking the backend to issue one first if the cookie is missing. */
async function csrfToken(): Promise<string> {
  let token = readCookie(CSRF_COOKIE);
  if (!token) {
    await fetch("/api/v1/auth/csrf", { credentials: "same-origin", cache: "no-store" });
    token = readCookie(CSRF_COOKIE);
  }
  if (!token) {
    throw new ApiError(0, "CSRF_UNAVAILABLE", "Could not start a secure session. Please reload the page.");
  }
  return token;
}

function fallbackMessage(status: number): string {
  if (status === 401) return "Please log in to continue.";
  if (status === 403) return "You do not have permission to do this.";
  if (status === 404) return "Not found.";
  if (status >= 500) return "Something went wrong on our side. Please try again.";
  return "The request could not be completed.";
}

export async function api<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const method = options.method ?? "GET";
  const headers: Record<string, string> = { Accept: "application/json" };
  let body: BodyInit | undefined;

  if (options.json !== undefined) {
    headers["Content-Type"] = "application/json";
    body = JSON.stringify(options.json);
  } else if (options.form) {
    body = new URLSearchParams(options.form);
  }
  if (method !== "GET") {
    headers[CSRF_HEADER] = await csrfToken();
  }

  let response: Response;
  try {
    response = await fetch(path, { method, headers, body, credentials: "same-origin", cache: "no-store" });
  } catch {
    throw new ApiError(0, "NETWORK_ERROR", "Cannot reach CampusNest. Check your connection and try again.");
  }

  if (response.status === 204) {
    return undefined as T;
  }
  const data: unknown = await response.json().catch(() => null);
  if (!response.ok) {
    const err = (data ?? {}) as ApiErrorBody;
    throw new ApiError(
      response.status,
      err.error ?? `HTTP_${response.status}`,
      err.message ?? fallbackMessage(response.status),
      err.fields ?? {},
    );
  }
  return data as T;
}

/** Turns any thrown value into a message safe to show the user. */
export function errorMessage(error: unknown): string {
  return error instanceof ApiError ? error.message : "Something went wrong. Please try again.";
}
