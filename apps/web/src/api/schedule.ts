import {
  WeeklyScheduleSchema,
  ScheduleSummarySchema,
  AdminScheduleSchema,
  type WeeklySchedule,
  type ScheduleSummary,
  type ScheduleDay,
  type AdminSchedule,
} from "@company/shared";
import { apiFetch } from "./client";

export async function getMySchedule(token: string): Promise<WeeklySchedule> {
  const data = await apiFetch<unknown>("/schedules/me", { token });
  return WeeklyScheduleSchema.parse(data);
}

export async function saveMySchedule(token: string, days: ScheduleDay[]): Promise<WeeklySchedule> {
  const data = await apiFetch<unknown>("/schedules/me", {
    token,
    method: "PUT",
    body: JSON.stringify({ days }),
  });
  return WeeklyScheduleSchema.parse(data);
}

export async function getMySummary(token: string): Promise<ScheduleSummary> {
  const data = await apiFetch<unknown>("/schedules/me/summary", { token });
  return ScheduleSummarySchema.parse(data);
}
export async function getAdminSchedules(token: string): Promise<AdminSchedule> {
  const data = await apiFetch<unknown>("/admin/schedules", { token });
  return AdminScheduleSchema.parse(data);
}