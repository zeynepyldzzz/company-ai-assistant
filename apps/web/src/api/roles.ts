import {
  AdminRoleListSchema,
  EmployeeRoleResponseSchema,
  type AdminRole,
  type EmployeeRoleResponse,
  type UpdateRoleRequest,
} from "@company/shared";
import { apiFetch } from "./client";

// C-11 (#85): GET /admin/roles, PUT /admin/users/{id}/roles - yalnizca system_admin.
export async function listRoles(token: string): Promise<AdminRole[]> {
  const data = await apiFetch<unknown>("/admin/roles", { token });
  return AdminRoleListSchema.parse(data);
}

export async function updateUserRole(
  employeeId: number,
  body: UpdateRoleRequest,
  token: string
): Promise<EmployeeRoleResponse> {
  const data = await apiFetch<unknown>(`/admin/users/${employeeId}/roles`, {
    method: "PUT",
    token,
    body: JSON.stringify(body),
  });
  return EmployeeRoleResponseSchema.parse(data);
}
