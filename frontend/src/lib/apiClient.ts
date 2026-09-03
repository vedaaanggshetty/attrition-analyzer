// Frontend talks ONLY to the API Gateway (port 8080) - never to an
// individual microservice directly. Every other service is reached only
// through routes the Gateway proxies.
const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? "http://localhost:8080";

const TOKEN_STORAGE_KEY = "attritionAnalyzer.token";

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_STORAGE_KEY);
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_STORAGE_KEY, token);
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_STORAGE_KEY);
}

/** AuthContext listens for this to end the session (US-05: expired/invalid
 * token on any protected request logs the user out, not just page load). */
export const SESSION_EXPIRED_EVENT = "attritionAnalyzer:sessionExpired";

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

interface RequestOptions {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: unknown;
  /** Attach the stored JWT as a Bearer token. Defaults to true. */
  authenticated?: boolean;
}

/**
 * Thin fetch wrapper for the Gateway's JSON API. Every backend error
 * response in this project follows the same shape - {message: string, ...}
 * (see authentication-service/user-profile-service's ErrorResponse) - so
 * ApiError surfaces that message directly for forms to display.
 */
export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = "GET", body, authenticated = true } = options;

  const headers: Record<string, string> = {};
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (authenticated) {
    const token = getToken();
    if (token) headers["Authorization"] = `Bearer ${token}`;
  }

  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, "Can't reach the server. Check your connection and try again.");
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    const message = (data && typeof data.message === "string" && data.message) || "Something went wrong.";
    if (response.status === 401 && authenticated) {
      window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
    }
    throw new ApiError(response.status, message);
  }

  return data as T;
}
