import { z } from "zod";
import { pagedResponseSchema } from "./directory";

// GET /shuttle-routes
export const ShuttleRouteSchema = z.object({
  id: z.number(),
  name: z.string(),
  plateNumber: z.string().nullable(),
  driverName: z.string().nullable(),
  driverPhone: z.string().nullable(),
});
export type ShuttleRoute = z.infer<typeof ShuttleRouteSchema>;
export const ShuttleRoutePagedResponseSchema = pagedResponseSchema(ShuttleRouteSchema);
export type ShuttleRoutePagedResponse = z.infer<typeof ShuttleRoutePagedResponseSchema>;

// GET /shuttle-routes/{id}/stops
export const ShuttleStopSchema = z.object({
  id: z.number(),
  name: z.string(),
  time: z.string().nullable(),
  orderIndex: z.number(),
  latitude: z.number().nullable(),
  longitude: z.number().nullable(),
});
export type ShuttleStop = z.infer<typeof ShuttleStopSchema>;
export const ShuttleStopListSchema = z.array(ShuttleStopSchema);
export type ShuttleStopList = z.infer<typeof ShuttleStopListSchema>;

// POST/PUT /admin/shuttle-routes response
export const ShuttleRouteDetailSchema = ShuttleRouteSchema.extend({
  stops: ShuttleStopListSchema,
});
export type ShuttleRouteDetail = z.infer<typeof ShuttleRouteDetailSchema>;

// GET /shuttle-routes/{id}/geometry (B-28)
export const ShuttleRouteGeometrySchema = z.object({
  routeId: z.number(),
  coordinates: z.array(z.object({ lat: z.number(), lng: z.number() })),
});
export type ShuttleRouteGeometry = z.infer<typeof ShuttleRouteGeometrySchema>;

// GET /shuttle-routes/{id}/plate
export const ShuttleRoutePlateSchema = z.object({
  id: z.number(),
  name: z.string(),
  plateNumber: z.string().nullable(),
});
export type ShuttleRoutePlate = z.infer<typeof ShuttleRoutePlateSchema>;

// POST/PUT /admin/shuttle-routes govdesi (B-13/B-14/B-15)
export const AdminShuttleStopRequestSchema = z.object({
  name: z.string().min(1),
  time: z.string().min(1),
  orderIndex: z.number(),
  latitude: z.number().nullable().optional(),
  longitude: z.number().nullable().optional(),
});
export type AdminShuttleStopRequest = z.infer<typeof AdminShuttleStopRequestSchema>;

// B-31: haritada cizilip OSRM Match ile yola oturtulmus rota noktasi.
export const RoutePointSchema = z.object({ lat: z.number(), lng: z.number() });
export type RoutePoint = z.infer<typeof RoutePointSchema>;

export const AdminShuttleRouteRequestSchema = z.object({
  name: z.string().min(1),
  plateNumber: z.string().nullable().optional(),
  driverName: z.string().nullable().optional(),
  driverPhone: z.string().nullable().optional(),
  stops: z.array(AdminShuttleStopRequestSchema).min(1),
  geometryPoints: z.array(RoutePointSchema).nullable().optional(),
});
export type AdminShuttleRouteRequest = z.infer<typeof AdminShuttleRouteRequestSchema>;

// POST /admin/shuttle-routes/match-geometry govdesi
export const RouteMatchRequestSchema = z.object({
  points: z.array(RoutePointSchema).min(2),
});
export type RouteMatchRequest = z.infer<typeof RouteMatchRequestSchema>;

// POST /admin/shuttle-routes/match-geometry yaniti
export const RouteMatchResponseSchema = z.object({
  coordinates: z.array(RoutePointSchema),
});
export type RouteMatchResponse = z.infer<typeof RouteMatchResponseSchema>;

// GET /admin/shuttle-routes/{id}/geometry-points (duzenleme ekraninda kayitli
// manuel rota noktalarini geri yuklemek icin - hicbir zaman fallback donmez)
export const GeometryPointListSchema = z.array(RoutePointSchema);
export type GeometryPointList = z.infer<typeof GeometryPointListSchema>;

// GET /shuttle-routes/address-suggestions?q= (A-33)
export const AddressSuggestionSchema = z.object({
  label: z.string(),
  lat: z.number(),
  lng: z.number(),
});
export type AddressSuggestion = z.infer<typeof AddressSuggestionSchema>;
export const AddressSuggestionListSchema = z.array(AddressSuggestionSchema);
export type AddressSuggestionList = z.infer<typeof AddressSuggestionListSchema>;

// GET /shuttle-routes/recommendation?lat=&lng= (B-6)
export const ShuttleRecommendationSchema = z.object({
  routeId: z.number(),
  routeName: z.string(),
  plateNumber: z.string().nullable(),
  driverName: z.string().nullable(),
  driverPhone: z.string().nullable(),
  stopId: z.number(),
  stopName: z.string(),
  distanceKm: z.number(),
  estimatedMinutes: z.number(),
  searchLat: z.number(),
  searchLng: z.number(),
});
export type ShuttleRecommendation = z.infer<typeof ShuttleRecommendationSchema>;
