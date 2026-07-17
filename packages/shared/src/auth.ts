import { z } from "zod";

// Roller: docs/apiEndpoints.md #0 Genel Kurallar
export const RoleSchema = z.enum(["employee", "admin"]);
export type Role = z.infer<typeof RoleSchema>;

export const AdminSubRoleSchema = z.enum([
  "hr_admin",
  "fleet_admin",
  "shuttle_admin",
  "canteen_admin",
  "system_admin",
]);
export type AdminSubRole = z.infer<typeof AdminSubRoleSchema>;

// GET /me
export const UserSchema = z.object({
  id: z.number(),
  name: z.string(),
  email: z.string().email(),
  role: RoleSchema,
  subRole: AdminSubRoleSchema.nullable(),
});
export type User = z.infer<typeof UserSchema>;

// POST /auth/login
export const LoginRequestSchema = z.object({
  email: z.string().email(),
  password: z.string().min(1),
});
export type LoginRequest = z.infer<typeof LoginRequestSchema>;

export const LoginResponseSchema = z.object({
  accessToken: z.string(),
  refreshToken: z.string(),
  user: UserSchema,
});
export type LoginResponse = z.infer<typeof LoginResponseSchema>;
