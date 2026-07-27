import { z } from "zod";

// GET /vehicles, POST/PUT /admin/vehicles, PUT /admin/vehicles/{id}/maintenance-status (B-8, B-9)

export const MaintenanceStatusSchema = z.enum(["available", "maintenance"]);
export type MaintenanceStatus = z.infer<typeof MaintenanceStatusSchema>;

export const VehicleSchema = z.object({
  id: z.number(),
  plate: z.string(),
  model: z.string().nullable(),
  maintenanceStatus: MaintenanceStatusSchema,
});
export type Vehicle = z.infer<typeof VehicleSchema>;

export const VehicleRequestSchema = z.object({
  plate: z.string().min(1),
  model: z.string().optional(),
  maintenanceStatus: MaintenanceStatusSchema.optional(),
});
export type VehicleRequest = z.infer<typeof VehicleRequestSchema>;

export const MaintenanceStatusUpdateRequestSchema = z.object({
  maintenanceStatus: MaintenanceStatusSchema,
});
export type MaintenanceStatusUpdateRequest = z.infer<typeof MaintenanceStatusUpdateRequestSchema>;
