import {
  VehicleSchema,
  ReservationListSchema,
  ReservationSchema,
  type Vehicle,
  type VehicleRequest,
  type MaintenanceStatusUpdateRequest,
  type ReservationList,
  type Reservation,
  type ReservationCreateRequest,
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

export async function listMyReservations(token: string): Promise<ReservationList> {
  const data = await apiFetch<unknown>("/reservations/me", { token });
  return ReservationListSchema.parse(data);
}

export async function createReservation(
  body: ReservationCreateRequest,
  token: string
): Promise<Reservation> {
  const data = await apiFetch<unknown>("/reservations", {
    method: "POST",
    body: JSON.stringify(body),
    token,
  });
  return ReservationSchema.parse(data);
}

export async function cancelReservation(id: number, token: string): Promise<void> {
  await apiFetch<void>(`/reservations/${id}`, { method: "DELETE", token });
}
