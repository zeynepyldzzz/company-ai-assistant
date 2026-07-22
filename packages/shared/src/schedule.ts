import { z } from "zod";

// GET/PUT /schedules/me, GET /schedules/me/summary (C-4)

export const ScheduleStatusSchema = z.enum(["office", "remote", "leave"]);
export type ScheduleStatus = z.infer<typeof ScheduleStatusSchema>;

export const WorkDaySchema = z.enum(["monday", "tuesday", "wednesday", "thursday", "friday"]);
export type WorkDay = z.infer<typeof WorkDaySchema>;

export const ScheduleDaySchema = z.object({
  day: WorkDaySchema,
  status: ScheduleStatusSchema,
});
export type ScheduleDay = z.infer<typeof ScheduleDaySchema>;

export const WeeklyScheduleSchema = z.object({
  weekStartDate: z.string(),
  // Kayit yoksa API bos dizi doner (C-4 tasarimi)
  days: z.array(ScheduleDaySchema),
});
export type WeeklySchedule = z.infer<typeof WeeklyScheduleSchema>;

export const ScheduleSummarySchema = z.object({
  office: z.number(),
  remote: z.number(),
  leave: z.number(),
});
export type ScheduleSummary = z.infer<typeof ScheduleSummarySchema>;