import type { User } from "@company/shared";

const STORAGE_KEY = "auth";

export interface StoredAuth {
  accessToken: string;
  refreshToken: string;
  user: User;
}

type Listener = (auth: StoredAuth | null) => void;
const listeners = new Set<Listener>();

export function readAuth(): StoredAuth | null {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as StoredAuth;
  } catch {
    return null;
  }
}

export function writeAuth(auth: StoredAuth): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(auth));
  listeners.forEach((listener) => listener(auth));
}

// apiFetch cagirir: access token suresi dolup arka planda yenilendiginde
// React context'in ve sonraki isteklerin guncel token'i gormesi icin.
export function updateTokens(accessToken: string, refreshToken: string): void {
  const current = readAuth();
  if (!current) return;
  writeAuth({ ...current, accessToken, refreshToken });
}

export function clearAuth(): void {
  localStorage.removeItem(STORAGE_KEY);
  listeners.forEach((listener) => listener(null));
}

export function subscribeAuth(listener: Listener): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}
