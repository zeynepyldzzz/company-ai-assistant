import {
  VehicleListSchema,
  ReservationListSchema,
  ReservationSchema,
  type VehicleList,
  type ReservationList,
  type Reservation,
  type ReservationCreateRequest,
} from "@company/shared";
import { apiFetch } from "./client";

export async function listVehicles(available: boolean, token: string): Promise<VehicleList> {
  const data = await apiFetch<unknown>(`/vehicles?available=${available}`, { token });
  return VehicleListSchema.parse(data);
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
