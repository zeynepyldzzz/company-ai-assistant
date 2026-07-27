import {
  VehicleSchema,
  type MaintenanceStatusUpdateRequest,
  type Vehicle,
  type VehicleRequest,
} from "@company/shared";
import { apiFetch } from "./client";

export async function listVehicles(token: string, available?: boolean): Promise<Vehicle[]> {
  const query = available === undefined ? "" : `?available=${available}`;
  const data = await apiFetch<unknown>(`/vehicles${query}`, { token });
  return VehicleSchema.array().parse(data);
}

export async function createVehicle(token: string, request: VehicleRequest): Promise<Vehicle> {
  const data = await apiFetch<unknown>("/admin/vehicles", {
    token,
    method: "POST",
    body: JSON.stringify(request),
  });
  return VehicleSchema.parse(data);
}

export async function updateVehicle(
  token: string,
  id: number,
  request: VehicleRequest
): Promise<Vehicle> {
  const data = await apiFetch<unknown>(`/admin/vehicles/${id}`, {
    token,
    method: "PUT",
    body: JSON.stringify(request),
  });
  return VehicleSchema.parse(data);
}

export async function updateMaintenanceStatus(
  token: string,
  id: number,
  request: MaintenanceStatusUpdateRequest
): Promise<Vehicle> {
  const data = await apiFetch<unknown>(`/admin/vehicles/${id}/maintenance-status`, {
    token,
    method: "PUT",
    body: JSON.stringify(request),
  });
  return VehicleSchema.parse(data);
}
