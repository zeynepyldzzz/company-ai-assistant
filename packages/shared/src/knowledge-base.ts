import { z } from "zod";
import { pagedResponseSchema } from "./directory";

// A-7 (issue #43) — A-6 backend'inin (hr/PolicyDocumentController) React UI'i.
// /admin/knowledge-base/documents uclari, yalnizca hr_admin / system_admin.
// Tarihler string: LocalDate "yyyy-MM-dd", Instant ISO-8601.

// policy_version.steps JSONB elemani (backend: hr/ProcedureStep record)
export const ProcedureStepSchema = z.object({
  order: z.number().int(),
  title: z.string(),
  detail: z.string(),
});
export type ProcedureStep = z.infer<typeof ProcedureStepSchema>;

// GET /admin/knowledge-base/documents liste elemani (backend: DocumentSummary).
// currentVersionNo/currentEffectiveDate guncel versiyonu olmayan dokuman icin null.
export const DocumentSummarySchema = z.object({
  id: z.number(),
  procedureId: z.number(),
  title: z.string(),
  procedureCategory: z.string(),
  currentVersionNo: z.number().nullable(),
  currentEffectiveDate: z.string().nullable(),
  createdAt: z.string(),
});
export type DocumentSummary = z.infer<typeof DocumentSummarySchema>;
export const DocumentSummaryPagedResponseSchema = pagedResponseSchema(DocumentSummarySchema);
export type DocumentSummaryPagedResponse = z.infer<typeof DocumentSummaryPagedResponseSchema>;

// GET .../{id}/versions elemani; POST/PUT yaniti da bu (backend: PolicyVersionResponse).
// content NotBlank degil -> null olabilir; steps JSONB -> null olabilir.
export const PolicyVersionSchema = z.object({
  id: z.number(),
  documentId: z.number(),
  versionNo: z.number(),
  content: z.string().nullable(),
  steps: z.array(ProcedureStepSchema).nullable(),
  effectiveDate: z.string(),
  isCurrent: z.boolean(),
  createdAt: z.string(),
  createdBy: z.number().nullable(),
});
export type PolicyVersion = z.infer<typeof PolicyVersionSchema>;
export const PolicyVersionPagedResponseSchema = pagedResponseSchema(PolicyVersionSchema);
export type PolicyVersionPagedResponse = z.infer<typeof PolicyVersionPagedResponseSchema>;

// GET /hr/procedures liste elemani (backend: HrProcedureSummary).
// A-7 formunda procedureId secimi icin kullanilir (dropdown). Ekstra alanlar yok sayilir.
export const HrProcedureSummarySchema = z.object({
  id: z.number(),
  title: z.string(),
  category: z.string(),
});
export type HrProcedureSummary = z.infer<typeof HrProcedureSummarySchema>;
export const HrProcedureSummaryPagedResponseSchema = pagedResponseSchema(HrProcedureSummarySchema);
export type HrProcedureSummaryPagedResponse = z.infer<typeof HrProcedureSummaryPagedResponseSchema>;

// POST /admin/knowledge-base/documents govdesi (backend: DocumentCreateRequest).
// procedureId/title/effectiveDate zorunlu; content/steps opsiyonel.
export const DocumentCreateRequestSchema = z.object({
  procedureId: z.number().int(),
  title: z.string().min(1),
  content: z.string().optional(),
  steps: z.array(ProcedureStepSchema).optional(),
  effectiveDate: z.string().min(1),
});
export type DocumentCreateRequest = z.infer<typeof DocumentCreateRequestSchema>;

// PUT /admin/knowledge-base/documents/{id} govdesi (backend: VersionCreateRequest).
export const VersionCreateRequestSchema = z.object({
  content: z.string().optional(),
  steps: z.array(ProcedureStepSchema).optional(),
  effectiveDate: z.string().min(1),
});
export type VersionCreateRequest = z.infer<typeof VersionCreateRequestSchema>;
