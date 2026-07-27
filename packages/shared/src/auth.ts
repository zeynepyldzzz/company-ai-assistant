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

// POST /auth/refresh
export const RefreshResponseSchema = z.object({
  accessToken: z.string(),
  refreshToken: z.string(),
});
export type RefreshResponse = z.infer<typeof RefreshResponseSchema>;

// POST /auth/login: admin rolu icin normal token yerine bu donuyor,
// devaminda POST /auth/2fa/verify cagrilmasi gerekiyor.
export const TwoFactorChallengeSchema = z.object({
  twoFactorRequired: z.literal(true),
  challengeToken: z.string(),
});
export type TwoFactorChallenge = z.infer<typeof TwoFactorChallengeSchema>;

// POST /auth/2fa/verify
export const TwoFactorVerifyRequestSchema = z.object({
  challengeToken: z.string(),
  code: z.string().min(1),
});
export type TwoFactorVerifyRequest = z.infer<typeof TwoFactorVerifyRequestSchema>;

// GET /admin/roles (C-11 #85)
export const AdminRoleSchema = z.object({
  id: z.number(),
  name: z.string(),
});
export type AdminRole = z.infer<typeof AdminRoleSchema>;

// PUT /admin/users/{id}/roles govdesi (C-11 #85)
export const UpdateRoleRequestSchema = z.object({
  roleId: z.number(),
});
export type UpdateRoleRequest = z.infer<typeof UpdateRoleRequestSchema>;

// PUT /admin/users/{id}/roles yaniti
export const EmployeeRoleResponseSchema = z.object({
  employeeId: z.number(),
  employeeName: z.string(),
  roleId: z.number(),
  roleName: z.string(),
});
export type EmployeeRoleResponse = z.infer<typeof EmployeeRoleResponseSchema>;
