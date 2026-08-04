import {
  ChangePasswordRequestSchema,
  LoginRequestSchema,
  LoginResponseSchema,
  TwoFactorChallengeSchema,
  UserSchema,
  type ChangePasswordRequest,
  type LoginRequest,
  type LoginResponse,
  type TwoFactorChallenge,
  type User,
} from "@company/shared";
import { apiFetch } from "./client";

// Admin rolu icin backend token yerine 2FA challenge donuyor (docs/apiEndpoints.md #0).
export async function login(credentials: LoginRequest): Promise<LoginResponse | TwoFactorChallenge> {
  const payload = LoginRequestSchema.parse(credentials);
  const data = await apiFetch<unknown>("/auth/login", {
    method: "POST",
    body: JSON.stringify(payload),
  });
  if (typeof data === "object" && data !== null && "twoFactorRequired" in data) {
    return TwoFactorChallengeSchema.parse(data);
  }
  return LoginResponseSchema.parse(data);
}

export async function verifyTwoFactor(challengeToken: string, code: string): Promise<LoginResponse> {
  const data = await apiFetch<unknown>("/auth/2fa/verify", {
    method: "POST",
    body: JSON.stringify({ challengeToken, code }),
  });
  return LoginResponseSchema.parse(data);
}

export async function getMe(token: string): Promise<User> {
  const data = await apiFetch<unknown>("/me", { token });
  return UserSchema.parse(data);
}

export async function logout(refreshToken: string): Promise<void> {
  await apiFetch<void>("/auth/logout", {
    method: "POST",
    body: JSON.stringify({ refreshToken }),
  });
}

// POST /auth/password (A-29): mevcut sifre dogrulanir, yeni sifre yazilir ve
// mustChangePassword false olur. Basarida govde donmez (204).
export async function changePassword(body: ChangePasswordRequest, token: string): Promise<void> {
  const payload = ChangePasswordRequestSchema.parse(body);
  await apiFetch<void>("/auth/password", {
    method: "POST",
    token,
    body: JSON.stringify(payload),
  });
}
