import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import type { ReactNode } from "react";
import { clearToken, getToken, setToken, SESSION_EXPIRED_EVENT } from "../lib/apiClient";
import { decodeJwt, isExpired } from "../lib/jwt";
import * as authApi from "../lib/authApi";

interface AuthUser {
  userId: string;
  email: string;
  role: string;
  /** Fetched separately from /users/me after login/load - null until it
   * resolves, so the sidebar/greeting can fall back to the email meanwhile. */
  fullName: string | null;
}

interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  /** Set once when a session ends because it expired (US-05), not on a
   * deliberate logout - Login reads this to explain why it landed there. */
  sessionExpired: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (fullName: string, email: string, password: string, phone?: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function userFromToken(token: string | null): AuthUser | null {
  if (!token) return null;
  const payload = decodeJwt(token);
  if (!payload || isExpired(payload) || !payload.email || !payload.role) return null;
  return { userId: payload.sub, email: payload.email, role: payload.role, fullName: null };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => userFromToken(getToken()));
  const [sessionExpired, setSessionExpired] = useState(false);
  const expiryTimer = useRef<number | undefined>(undefined);

  const logout = useCallback(() => {
    window.clearTimeout(expiryTimer.current);
    clearToken();
    setUser(null);
  }, []);

  const scheduleExpiry = useCallback(
    (token: string) => {
      window.clearTimeout(expiryTimer.current);
      const payload = decodeJwt(token);
      if (!payload?.exp) return;
      const msRemaining = payload.exp * 1000 - Date.now();
      if (msRemaining <= 0) {
        logout();
        return;
      }
      expiryTimer.current = window.setTimeout(() => {
        setSessionExpired(true);
        logout();
      }, msRemaining);
    },
    [logout]
  );

  // US-05: a token already expired on page load, or one that expires while
  // the tab stays open, both end the session the same way.
  useEffect(() => {
    const token = getToken();
    if (token) scheduleExpiry(token);
    return () => window.clearTimeout(expiryTimer.current);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Any protected request that comes back 401 (expired/invalid/revoked
  // token) ends the session the same way, not just the local expiry timer.
  useEffect(() => {
    const onSessionExpired = () => {
      setSessionExpired(true);
      logout();
    };
    window.addEventListener(SESSION_EXPIRED_EVENT, onSessionExpired);
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, onSessionExpired);
  }, [logout]);

  // One shared /users/me fetch for the whole app - the sidebar and the
  // Dashboard greeting both read user.fullName instead of each fetching it.
  useEffect(() => {
    if (!user || user.fullName) return;
    const userId = user.userId;
    authApi
      .getMyProfile()
      .then((profile) => {
        setUser((current) => (current && current.userId === userId ? { ...current, fullName: profile.fullName } : current));
      })
      .catch(() => {
        // Sidebar/greeting fall back to the email-derived name; not worth surfacing an error for.
      });
  }, [user]);

  const login = useCallback(
    async (email: string, password: string) => {
      const response = await authApi.login({ email, password });
      setToken(response.token);
      setUser(userFromToken(response.token));
      setSessionExpired(false);
      scheduleExpiry(response.token);
    },
    [scheduleExpiry]
  );

  const register = useCallback(
    async (fullName: string, email: string, password: string, phone?: string) => {
      await authApi.register({ fullName, email, password, phone });
      // Registration doesn't return a token - log in immediately after with
      // the same credentials so the flow still lands the user in the app.
      await login(email, password);
    },
    [login]
  );

  const value = useMemo<AuthContextValue>(
    () => ({ user, isAuthenticated: user !== null, sessionExpired, login, register, logout }),
    [user, sessionExpired, login, register, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within an AuthProvider");
  return ctx;
}
