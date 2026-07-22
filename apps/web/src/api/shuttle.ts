import {
  ShuttleRoutePagedResponseSchema,
  ShuttleStopListSchema,
  ShuttleRoutePlateSchema,
  ShuttleRecommendationSchema,
  type ShuttleRoutePagedResponse,
  type ShuttleStopList,
  type ShuttleRoutePlate,
  type ShuttleRecommendation,
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

export async function getShuttlePlate(routeId: number, token: string): Promise<ShuttleRoutePlate> {
  const data = await apiFetch<unknown>(`/shuttle-routes/${routeId}/plate`, { token });
  return ShuttleRoutePlateSchema.parse(data);
}

export async function getShuttleRecommendation(
  lat: number,
  lng: number,
  token: string
): Promise<ShuttleRecommendation> {
  const data = await apiFetch<unknown>(`/shuttle-routes/recommendation${buildQuery({ lat, lng })}`, {
    token,
  });
  return ShuttleRecommendationSchema.parse(data);
}
