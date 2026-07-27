import {
  DocumentSummaryPagedResponseSchema,
  HrProcedureSummaryPagedResponseSchema,
  PolicyVersionSchema,
  PolicyVersionPagedResponseSchema,
  type DocumentCreateRequest,
  type DocumentSummaryPagedResponse,
  type HrProcedureSummary,
  type PolicyVersion,
  type PolicyVersionPagedResponse,
  type VersionCreateRequest,
} from "@company/shared";
import { apiFetch } from "./client";

const BASE = "/admin/knowledge-base/documents";

function buildQuery(params: Record<string, string | number | undefined>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== "") search.set(key, String(value));
  }
  const query = search.toString();
  return query ? `?${query}` : "";
}

export async function listDocuments(
  params: { page?: number; pageSize?: number },
  token: string
): Promise<DocumentSummaryPagedResponse> {
  const data = await apiFetch<unknown>(`${BASE}${buildQuery(params)}`, { token });
  return DocumentSummaryPagedResponseSchema.parse(data);
}

// POST -> olusan v1 versiyonu (201), PolicyVersionResponse doner
export async function createDocument(
  body: DocumentCreateRequest,
  token: string
): Promise<PolicyVersion> {
  const data = await apiFetch<unknown>(BASE, {
    method: "POST",
    token,
    body: JSON.stringify(body),
  });
  return PolicyVersionSchema.parse(data);
}

// PUT /{id} -> yeni versiyon (is_current=true) doner
export async function addVersion(
  id: number,
  body: VersionCreateRequest,
  token: string
): Promise<PolicyVersion> {
  const data = await apiFetch<unknown>(`${BASE}/${id}`, {
    method: "PUT",
    token,
    body: JSON.stringify(body),
  });
  return PolicyVersionSchema.parse(data);
}

// DELETE /{id} -> 204 No Content
export async function deleteDocument(id: number, token: string): Promise<void> {
  await apiFetch<void>(`${BASE}/${id}`, { method: "DELETE", token });
}

// GET /{id}/versions -> kronolojik versiyon gecmisi (zarf, page=0)
export async function listVersions(
  id: number,
  token: string
): Promise<PolicyVersionPagedResponse> {
  const data = await apiFetch<unknown>(`${BASE}/${id}/versions`, { token });
  return PolicyVersionPagedResponseSchema.parse(data);
}

// GET /hr/procedures -> doku man formunda procedureId secimi icin (dropdown).
// Tek sayfada tum prosedurleri cekmek yeterli (kucuk kume).
export async function listProcedures(token: string): Promise<HrProcedureSummary[]> {
  const data = await apiFetch<unknown>(`/hr/procedures?page=0&pageSize=200`, { token });
  return HrProcedureSummaryPagedResponseSchema.parse(data).data;
}
