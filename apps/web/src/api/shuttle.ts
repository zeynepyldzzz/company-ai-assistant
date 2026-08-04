import {
  ShuttleRoutePagedResponseSchema,
  ShuttleStopListSchema,
  ShuttleRouteGeometrySchema,
  ShuttleRoutePlateSchema,
  ShuttleRouteDetailSchema,
  ShuttleRecommendationSchema,
  type ShuttleRoutePagedResponse,
  type ShuttleStopList,
  type ShuttleRouteGeometry,
  type ShuttleRoutePlate,
  type ShuttleRouteDetail,
  type ShuttleRecommendation,
  type AdminShuttleRouteRequest,
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

export async function listShuttleRoutes(
  params: { page?: number; pageSize?: number },
  token: string
): Promise<ShuttleRoutePagedResponse> {
  const data = await apiFetch<unknown>(`/shuttle-routes${buildQuery(params)}`, { token });
  return ShuttleRoutePagedResponseSchema.parse(data);
}

export async function getShuttleStops(routeId: number, token: string): Promise<ShuttleStopList> {
  const data = await apiFetch<unknown>(`/shuttle-routes/${routeId}/stops`, { token });
  return ShuttleStopListSchema.parse(data);
}

export async function getShuttleRouteGeometry(routeId: number, token: string): Promise<ShuttleRouteGeometry> {
  const data = await apiFetch<unknown>(`/shuttle-routes/${routeId}/geometry`, { token });
  return ShuttleRouteGeometrySchema.parse(data);
}

export async function getShuttlePlate(routeId: number, token: string): Promise<ShuttleRoutePlate> {
  const data = await apiFetch<unknown>(`/shuttle-routes/${routeId}/plate`, { token });
  return ShuttleRoutePlateSchema.parse(data);
}

export async function getShuttleRecommendation(
  params: { lat: number; lng: number } | { address: string },
  token: string
): Promise<ShuttleRecommendation> {
  const query = "address" in params ? { address: params.address } : { lat: params.lat, lng: params.lng };
  const data = await apiFetch<unknown>(`/shuttle-routes/recommendation${buildQuery(query)}`, {
    token,
  });
  return ShuttleRecommendationSchema.parse(data);
}

export async function deleteShuttleRoute(id: number, token: string): Promise<void> {
  await apiFetch<void>(`/admin/shuttle-routes/${id}`, { method: "DELETE", token });
}

// B-17: admin servis guzergahi CRUD (FR-73, shuttle_admin/system_admin).
export async function createShuttleRoute(
  body: AdminShuttleRouteRequest,
  token: string
): Promise<ShuttleRouteDetail> {
  const data = await apiFetch<unknown>("/admin/shuttle-routes", {
    method: "POST",
    token,
    body: JSON.stringify(body),
  });
  return ShuttleRouteDetailSchema.parse(data);
}

export async function updateShuttleRoute(
  id: number,
  body: AdminShuttleRouteRequest,
  token: string
): Promise<ShuttleRouteDetail> {
  const data = await apiFetch<unknown>(`/admin/shuttle-routes/${id}`, {
    method: "PUT",
    token,
    body: JSON.stringify(body),
  });
  return ShuttleRouteDetailSchema.parse(data);
}
