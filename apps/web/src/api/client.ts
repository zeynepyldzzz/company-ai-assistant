import { clearAuth, readAuth, updateTokens } from "@/auth/token-store";

export const API_BASE = "/api/v1";
const NO_REFRESH_PATHS = ["/auth/login", "/auth/refresh", "/auth/2fa/verify"];

export class ApiError extends Error {
  status: number;
  code: string;

  constructor(status: number, code: string, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
  }
}

interface RequestOptions extends RequestInit {
  token?: string | null;
}

// Backend refresh token'i rotasyonla tek kullanimlik yapiyor (kullanilan
// refresh token iptal edilip yenisi verilir). Ayni anda birden fazla istek
// 401 alirsa hepsi bu tek promise'i bekler; aksi halde ikinci refresh
// cagrisi ilk cagrinin (artik iptal edilmis) refresh token'ini kullanir ve
// kullaniciyi gereksiz yere oturumdan atar.
let refreshPromise: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      const stored = readAuth();
      if (!stored) return null;
      try {
        const response = await fetch(`${API_BASE}/auth/refresh`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ refreshToken: stored.refreshToken }),
        });
        if (!response.ok) {
          clearAuth();
          return null;
        }
        const data = (await response.json()) as { accessToken: string; refreshToken: string };
        updateTokens(data.accessToken, data.refreshToken);
        return data.accessToken;
      } catch {
        clearAuth();
        return null;
      }
    })().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

async function doFetch(path: string, token: string | null | undefined, rest: RequestInit) {
  return fetch(`${API_BASE}${path}`, {
    ...rest,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...rest.headers,
    },
  });
}

export async function apiFetch<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { token, headers, ...rest } = options;

  let response = await doFetch(path, token, { ...rest, headers });

  // Access token suresi dolmus olabilir: sessizce yenileyip isteği bir kez tekrar dene.
  if (response.status === 401 && token && !NO_REFRESH_PATHS.includes(path)) {
    const newToken = await refreshAccessToken();
    if (newToken) {
      response = await doFetch(path, newToken, { ...rest, headers });
    }
  }

  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as {
      error?: { code?: string; message?: string };
    } | null;
    throw new ApiError(
      response.status,
      body?.error?.code ?? "UNKNOWN_ERROR",
      body?.error?.message ?? response.statusText
    );
  }

  // 204 No Content (DELETE) veya bos govdeli 200 (POST /auth/logout gibi):
  // response.json() bos govdede patlar.
  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}
