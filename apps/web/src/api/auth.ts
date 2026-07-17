import {
  LoginRequestSchema,
  LoginResponseSchema,
  UserSchema,
  type LoginRequest,
  type LoginResponse,
  type User,
} from "@company/shared";
import { apiFetch } from "./client";

// TODO(ISSUE-003): bu isteğin bugün karşılığı olan gerçek bir backend endpoint'i yok
// (AuthController şu an sadece /auth/ping stub'ı içeriyor). Kontrat
// docs/apiEndpoints.md ile birebir uyumlu tutuldu; ISSUE-003 birleştiğinde
// bu dosyada değişiklik gerekmemeli.
export async function login(credentials: LoginRequest): Promise<LoginResponse> {
  const payload = LoginRequestSchema.parse(credentials);
  const data = await apiFetch<unknown>("/auth/login", {
    method: "POST",
    body: JSON.stringify(payload),
  });
  return LoginResponseSchema.parse(data);
}

export async function getMe(token: string): Promise<User> {
  const data = await apiFetch<unknown>("/me", { token });
  return UserSchema.parse(data);
}
