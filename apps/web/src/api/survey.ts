import {
  AdminSurveySchema,
  SurveySchema,
  SurveyResultsSchema,
  SurveyResponseCountSchema,
  type AdminSurvey,
  type AdminSurveyCreateRequest,
  type AdminSurveyUpdateRequest,
  type Survey,
  type SurveyResults,
  type SurveyResponseCount,
} from "@company/shared";
import { apiFetch } from "./client";

// FR-43: anonim geri bildirim. Kimlik hicbir sekilde gonderilmez (bilincli).
// C-13 (#121): sikli soruya oy verdikten sonra istege bagli olarak cagirilir.
export async function submitFeedback(
  content: string,
  surveyId: number | null,
  token: string
): Promise<void> {
  await apiFetch<void>("/feedback", {
    method: "POST",
    token,
    body: JSON.stringify({ surveyId, content }),
  });
}

const ADMIN_BASE = "/admin/surveys";

// FR-42: giris yapmis her calisan aktif anketleri gorebilir.
// C-13 (#121): artik deadline + sabit secenek listesi de doner.
export async function listActiveSurveys(token: string): Promise<Survey[]> {
  const data = await apiFetch<unknown[]>("/surveys/active", { token });
  return data.map((item) => SurveySchema.parse(item));
}

// POST /surveys/{id}/responses — C-13 (#121): serbest metin yerine secilen secenegin id'si.
export async function submitSurveyResponse(
  surveyId: number,
  optionId: number,
  token: string
): Promise<void> {
  await apiFetch<void>(`/surveys/${surveyId}/responses`, {
    method: "POST",
    token,
    body: JSON.stringify({ optionId }),
  });
}

// C-13 (#121): calisana acik response-count endpoint'i (dashboard progress bar icin).
export async function getSurveyResponseCount(
  surveyId: number,
  token: string
): Promise<SurveyResponseCount> {
  const data = await apiFetch<unknown>(`/surveys/${surveyId}/response-count`, { token });
  return SurveyResponseCountSchema.parse(data);
}

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

// PUT /admin/surveys/{id} — C-13 (#121): baslik, secenekler, gecerlilik (deadline) tarihi duzenlenir.
export async function updateSurvey(
  id: number,
  body: AdminSurveyUpdateRequest,
  token: string
): Promise<AdminSurvey> {
  const data = await apiFetch<unknown>(`${ADMIN_BASE}/${id}`, {
    method: "PUT",
    token,
    body: JSON.stringify(body),
  });
  return AdminSurveySchema.parse(data);
}

// DELETE /admin/surveys/{id} — C-13 (#121): anketi ve bagli tum kayitlari siler.
export async function deleteSurvey(id: number, token: string): Promise<void> {
  await apiFetch<void>(`${ADMIN_BASE}/${id}`, {
    method: "DELETE",
    token,
  });
}
