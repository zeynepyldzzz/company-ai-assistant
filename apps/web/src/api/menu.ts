import {
  MenuSchema,
  WeeklyMenuSchema,
  MenuImportResponseSchema,
  type Menu,
  type WeeklyMenu,
  type MenuImportResponse,
} from "@company/shared";
import { apiFetch, ApiError } from "./client";

export async function getTodayMenu(token: string): Promise<Menu> {
  const data = await apiFetch<unknown>("/menus/today", { token });
  return MenuSchema.parse(data);
}

export async function getWeeklyMenu(token: string): Promise<WeeklyMenu> {
  const data = await apiFetch<unknown>("/menus/weekly", { token });
  return WeeklyMenuSchema.parse(data);
}

// C-2: POST /admin/menus/import - dosya yuklemesi FormData/multipart oldugu icin
// apiFetch'in sabit "Content-Type: application/json" header'ini kullanamayiz,
// dogrudan fetch ile gonderiyoruz (report.ts'deki xlsx indirme deseniyle ayni).
export async function importMenuExcel(
  file: File,
  commit: boolean,
  token: string
): Promise<MenuImportResponse> {
  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch(`/api/v1/admin/menus/import?commit=${commit}`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
    body: formData,
  });

  if (!response.ok) {
    const message = await response.text().catch(() => "Menü içe aktarılamadı.");
    throw new ApiError(response.status, "MENU_IMPORT_FAILED", message || "Menü içe aktarılamadı.");
  }

  return MenuImportResponseSchema.parse(await response.json());
}

// C-2: DELETE /admin/menus/{id}
export async function deleteMenu(id: number, token: string): Promise<void> {
  await apiFetch<void>(`/admin/menus/${id}`, { method: "DELETE", token });
}