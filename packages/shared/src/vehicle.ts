import { z } from "zod";

// GET /vehicles?available=
export const MaintenanceStatusSchema = z.enum(["available", "maintenance"]);
export type MaintenanceStatus = z.infer<typeof MaintenanceStatusSchema>;

export const VehicleSchema = z.object({
  id: z.number(),
  plate: z.string(),
  model: z.string().nullable(),
  maintenanceStatus: MaintenanceStatusSchema,
});
export type Vehicle = z.infer<typeof VehicleSchema>;
export const VehicleListSchema = z.array(VehicleSchema);
export type VehicleList = z.infer<typeof VehicleListSchema>;

// GET /reservations/me (FR-40)
export const ReservationStatusSchema = z.enum(["confirmed", "cancelled"]);
export type ReservationStatus = z.infer<typeof ReservationStatusSchema>;

export const ReservationSchema = z.object({
  id: z.number(),
  vehicleId: z.number(),
  vehiclePlate: z.string(),
  startTime: z.string(),
  endTime: z.string(),
  status: ReservationStatusSchema,
});
export type Reservation = z.infer<typeof ReservationSchema>;
export const ReservationListSchema = z.array(ReservationSchema);
export type ReservationList = z.infer<typeof ReservationListSchema>;

// POST /reservations (FR-39)
export const ReservationCreateRequestSchema = z.object({
  vehicleId: z.number(),
  startTime: z.string(),
  endTime: z.string(),
});
export type ReservationCreateRequest = z.infer<typeof ReservationCreateRequestSchema>;
