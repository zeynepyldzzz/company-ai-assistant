import {
  LoginRequestSchema,
  LoginResponseSchema,
  TwoFactorChallengeSchema,
  UserSchema,
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
