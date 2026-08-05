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

export const AdminShuttleRouteRequestSchema = z.object({
  name: z.string().min(1),
  plateNumber: z.string().nullable().optional(),
  driverName: z.string().nullable().optional(),
  driverPhone: z.string().nullable().optional(),
  stops: z.array(AdminShuttleStopRequestSchema).min(1),
});
export type AdminShuttleRouteRequest = z.infer<typeof AdminShuttleRouteRequestSchema>;

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
});
export type ShuttleRecommendation = z.infer<typeof ShuttleRecommendationSchema>;
