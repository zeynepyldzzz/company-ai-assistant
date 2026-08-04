import {
  CreateEmployeeResponseSchema,
  DepartmentPagedResponseSchema,
  DepartmentSchema,
  EmployeePagedResponseSchema,
  EmployeeSchema,
  type AdminDepartmentRequest,
  type AdminEmployeeRequest,
  type CreateEmployeeResponse,
  type Department,
  type DepartmentPagedResponse,
  type Employee,
  type EmployeePagedResponse,
} from "@company/shared";
import { apiFetch } from "./client";

function buildQuery(params: Record<string, string | number | undefined>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== "") search.set(key, String(value));
  }
  const query = search.toString();
  return query ? `?${query}` : "";
}

export async function searchEmployees(
  params: { search?: string; department?: string; office?: string; page?: number; pageSize?: number },
  token: string
): Promise<EmployeePagedResponse> {
  const data = await apiFetch<unknown>(`/employees${buildQuery(params)}`, { token });
  return EmployeePagedResponseSchema.parse(data);
}

export async function getEmployeeById(id: number, token: string): Promise<Employee> {
  const data = await apiFetch<unknown>(`/employees/${id}`, { token });
  return EmployeeSchema.parse(data);
}

export async function searchDepartments(
  params: { search?: string; page?: number; pageSize?: number },
  token: string
): Promise<DepartmentPagedResponse> {
  const data = await apiFetch<unknown>(`/departments${buildQuery(params)}`, { token });
  return DepartmentPagedResponseSchema.parse(data);
}

export async function getDepartmentById(id: number, token: string): Promise<Department> {
  const data = await apiFetch<unknown>(`/departments/${id}`, { token });
  return DepartmentSchema.parse(data);
}

// #84 (Hafta 4): admin çalışan CRUD (FR-68-71, hr_admin/system_admin).
// A-29: sifre gonderilmezse backend uretir ve yanitta BIR KEZ doner.
export async function createEmployee(
  body: AdminEmployeeRequest,
  token: string
): Promise<CreateEmployeeResponse> {
  const data = await apiFetch<unknown>("/admin/employees", {
    method: "POST",
    token,
    body: JSON.stringify(body),
  });
  return CreateEmployeeResponseSchema.parse(data);
}

// A-29: yeni gecici sifre uretir ve BIR KEZ doner; kullanici ilk girisinde kendi
// sifresini belirler.
export async function resetEmployeePassword(
  id: number,
  token: string
): Promise<CreateEmployeeResponse> {
  const data = await apiFetch<unknown>(`/admin/employees/${id}/reset-password`, {
    method: "POST",
    token,
  });
  return CreateEmployeeResponseSchema.parse(data);
}

export async function updateEmployee(
  id: number,
  body: AdminEmployeeRequest,
  token: string
): Promise<Employee> {
  const data = await apiFetch<unknown>(`/admin/employees/${id}`, {
    method: "PUT",
    token,
    body: JSON.stringify(body),
  });
  return EmployeeSchema.parse(data);
}

export async function deleteEmployee(id: number, token: string): Promise<void> {
  await apiFetch<void>(`/admin/employees/${id}`, { method: "DELETE", token });
}

// #84 (Hafta 4): admin departman CRUD.
export async function createDepartment(body: AdminDepartmentRequest, token: string): Promise<Department> {
  const data = await apiFetch<unknown>("/admin/departments", {
    method: "POST",
    token,
    body: JSON.stringify(body),
  });
  return DepartmentSchema.parse(data);
}

export async function updateDepartment(
  id: number,
  body: AdminDepartmentRequest,
  token: string
): Promise<Department> {
  const data = await apiFetch<unknown>(`/admin/departments/${id}`, {
    method: "PUT",
    token,
    body: JSON.stringify(body),
  });
  return DepartmentSchema.parse(data);
}

export async function deleteDepartment(id: number, token: string): Promise<void> {
  await apiFetch<void>(`/admin/departments/${id}`, { method: "DELETE", token });
}
