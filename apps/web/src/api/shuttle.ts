import {
  ShuttleRoutePagedResponseSchema,
  ShuttleStopListSchema,
  ShuttleRouteGeometrySchema,
  ShuttleRoutePlateSchema,
  ShuttleRouteDetailSchema,
  ShuttleRecommendationSchema,
  AddressSuggestionListSchema,
  RouteMatchResponseSchema,
  GeometryPointListSchema,
  type ShuttleRoutePagedResponse,
  type ShuttleStopList,
  type ShuttleRouteGeometry,
  type ShuttleRoutePlate,
  type ShuttleRouteDetail,
  type ShuttleRecommendation,
  type AdminShuttleRouteRequest,
  type AddressSuggestionList,
  type RoutePoint,
  type RouteMatchResponse,
  type GeometryPointList,
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

export async function getAddressSuggestions(query: string, token: string): Promise<AddressSuggestionList> {
  const data = await apiFetch<unknown>(`/shuttle-routes/address-suggestions${buildQuery({ q: query })}`, {
    token,
  });
  return AddressSuggestionListSchema.parse(data);
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

// B-31: haritada cizilen ham noktalari OSRM Match ile gercek yola oturtan
// onizleme ucu - henuz kaydedilmemis (id'si olmayan) yeni bir guzergah
// icin de calisir.
export async function matchRouteGeometry(points: RoutePoint[], token: string): Promise<RouteMatchResponse> {
  const data = await apiFetch<unknown>("/admin/shuttle-routes/match-geometry", {
    method: "POST",
    token,
    body: JSON.stringify({ points }),
  });
  return RouteMatchResponseSchema.parse(data);
}

// B-31: duzenleme ekraninda kayitli manuel rota noktalarini geri yuklemek
// icin - public /geometry ucunun aksine kayit yoksa fallback donmez, sadece
// bos liste doner.
export async function getShuttleRouteGeometryPoints(routeId: number, token: string): Promise<GeometryPointList> {
  const data = await apiFetch<unknown>(`/admin/shuttle-routes/${routeId}/geometry-points`, { token });
  return GeometryPointListSchema.parse(data);
}
