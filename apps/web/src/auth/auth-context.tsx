import { createContext, useContext, useMemo, useState, type ReactNode } from "react";
import type { User } from "@company/shared";

const STORAGE_KEY = "auth";

interface StoredAuth {
  token: string;
  user: User;
}

interface AuthContextValue {
  token: string | null;
  user: User | null;
  setAuth: (auth: StoredAuth) => void;
  clearAuth: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function readStoredAuth(): StoredAuth | null {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as StoredAuth;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuthState] = useState<StoredAuth | null>(() => readStoredAuth());

  const value = useMemo<AuthContextValue>(
    () => ({
      token: auth?.token ?? null,
      user: auth?.user ?? null,
      setAuth: (next: StoredAuth) => {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
        setAuthState(next);
      },
      clearAuth: () => {
        localStorage.removeItem(STORAGE_KEY);
        setAuthState(null);
      },
    }),
    [auth]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return ctx;
}
