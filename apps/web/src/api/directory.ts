import {
  CallTriggerResponseSchema,
  DepartmentPagedResponseSchema,
  DepartmentSchema,
  EmployeePagedResponseSchema,
  EmployeeSchema,
  PhonebookEntryPagedResponseSchema,
  type CallTriggerResponse,
  type Department,
  type DepartmentPagedResponse,
  type Employee,
  type EmployeePagedResponse,
  type PhonebookEntryPagedResponse,
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

export async function searchPhonebook(
  params: { search?: string; page?: number; pageSize?: number },
  token: string
): Promise<PhonebookEntryPagedResponse> {
  const data = await apiFetch<unknown>(`/phonebook${buildQuery(params)}`, { token });
  return PhonebookEntryPagedResponseSchema.parse(data);
}

export async function triggerCall(extension: string, token: string): Promise<CallTriggerResponse> {
  const data = await apiFetch<unknown>(`/phonebook/${extension}/call`, {
    method: "POST",
    token,
  });
  return CallTriggerResponseSchema.parse(data);
}
