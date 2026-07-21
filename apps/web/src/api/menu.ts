import { MenuSchema, WeeklyMenuSchema, type Menu, type WeeklyMenu } from "@company/shared";
import { apiFetch } from "./client";

export async function getTodayMenu(token: string): Promise<Menu> {
  const data = await apiFetch<unknown>("/menus/today", { token });
  return MenuSchema.parse(data);
}

export async function getWeeklyMenu(token: string): Promise<WeeklyMenu> {
  const data = await apiFetch<unknown>("/menus/weekly", { token });
  return WeeklyMenuSchema.parse(data);
}