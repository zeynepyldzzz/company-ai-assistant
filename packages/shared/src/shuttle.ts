import { z } from "zod";
import { pagedResponseSchema } from "./directory";

// GET /shuttle-routes
export const ShuttleRouteSchema = z.object({
  id: z.number(),
  name: z.string(),
  plateNumber: z.string().nullable(),
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

// GET /shuttle-routes/{id}/plate
export const ShuttleRoutePlateSchema = z.object({
  id: z.number(),
  plateNumber: z.string().nullable(),
});
export type ShuttleRoutePlate = z.infer<typeof ShuttleRoutePlateSchema>;

// GET /shuttle-routes/recommendation?lat=&lng= (B-6)
export const ShuttleRecommendationSchema = z.object({
  routeId: z.number(),
  routeName: z.string(),
  plateNumber: z.string().nullable(),
  stopId: z.number(),
  stopName: z.string(),
  distanceKm: z.number(),
  estimatedMinutes: z.number(),
});
export type ShuttleRecommendation = z.infer<typeof ShuttleRecommendationSchema>;
