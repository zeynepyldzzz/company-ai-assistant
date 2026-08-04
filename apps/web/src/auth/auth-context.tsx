import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import type { User } from "@company/shared";
import {
  clearAuth as clearStoredAuth,
  readAuth,
  subscribeAuth,
  writeAuth,
  type StoredAuth,
} from "./token-store";

interface AuthContextValue {
  token: string | null;
  user: User | null;
  /** A-29: geçici şifreyle giriş yapıldıysa true; kullanıcı şifre değiştirmeye yönlendirilir. */
  mustChangePassword: boolean;
  setAuth: (auth: StoredAuth) => void;
  clearAuth: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuthState] = useState<StoredAuth | null>(() => readAuth());

  // apiFetch, access token suresi doldugunda arka planda yeniliyor veya
  // yenileme basarisiz olursa oturumu temizliyor (client.ts); bu component
  // disindaki degisiklikleri buradan yakalar.
  useEffect(() => subscribeAuth(setAuthState), []);

  const value = useMemo<AuthContextValue>(
    () => ({
      token: auth?.accessToken ?? null,
      user: auth?.user ?? null,
      mustChangePassword: auth?.mustChangePassword ?? false,
      setAuth: (next: StoredAuth) => writeAuth(next),
      clearAuth: () => clearStoredAuth(),
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
