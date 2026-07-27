import {
  AdminSurveySchema,
  SurveyResultsSchema,
  type AdminSurvey,
  type AdminSurveyCreateRequest,
  type SurveyResults,
} from "@company/shared";
import { apiFetch } from "./client";

const ADMIN_BASE = "/admin/surveys";

// GET /admin/surveys — taslak+yayimlanmis TUM anketler (admin listesi).
export async function listAdminSurveys(token: string): Promise<AdminSurvey[]> {
  const data = await apiFetch<unknown[]>(ADMIN_BASE, { token });
  return data.map((item) => AdminSurveySchema.parse(item));
}

// POST /admin/surveys — taslak (published=false) olarak olusturur.
export async function createSurvey(
  body: AdminSurveyCreateRequest,
  token: string
): Promise<AdminSurvey> {
  const data = await apiFetch<unknown>(ADMIN_BASE, {
    method: "POST",
    token,
    body: JSON.stringify(body),
  });
  return AdminSurveySchema.parse(data);
}

// PUT /admin/surveys/{id}/publish — yayimlar, GET /surveys/active'te gorunur hale gelir.
export async function publishSurvey(id: number, token: string): Promise<AdminSurvey> {
  const data = await apiFetch<unknown>(`${ADMIN_BASE}/${id}/publish`, {
    method: "PUT",
    token,
  });
  return AdminSurveySchema.parse(data);
}

// GET /admin/surveys/{id}/results — FR-44: sonuclari ozet halinde gorur.
export async function getSurveyResults(id: number, token: string): Promise<SurveyResults> {
  const data = await apiFetch<unknown>(`${ADMIN_BASE}/${id}/results`, { token });
  return SurveyResultsSchema.parse(data);
}
