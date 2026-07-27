import { UsageReportResponseSchema, type UsageReportResponse } from "@company/shared";
import { apiFetch } from "./client";

// C-11 (#85): GET /admin/reports/{type}, GET /admin/reports/{type}/export?format=xlsx - yalnizca system_admin.
export async function getUsageReport(token: string): Promise<UsageReportResponse> {
  const data = await apiFetch<unknown>("/admin/reports/usage", { token });
  return UsageReportResponseSchema.parse(data);
}

// Export ikili (xlsx) dosya dondurdugu icin apiFetch'in JSON parse'ini kullanamayiz,
// dogrudan fetch ile blob indiriyoruz.
export async function downloadUsageReportXlsx(token: string): Promise<Blob> {
  const response = await fetch("/api/v1/admin/reports/usage/export?format=xlsx", {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) {
    throw new Error("Rapor indirilemedi.");
  }
  return response.blob();
}
