import { z } from "zod";

// GET /admin/reports/{type} (C-11 #85). MVP'de yalnizca "usage" tipi var.
export const UsageReportRowSchema = z.object({
  module: z.string(),
  metric: z.string(),
  count: z.number(),
});
export type UsageReportRow = z.infer<typeof UsageReportRowSchema>;

export const UsageReportResponseSchema = z.object({
  type: z.string(),
  generatedAt: z.string(),
  rows: z.array(UsageReportRowSchema),
});
export type UsageReportResponse = z.infer<typeof UsageReportResponseSchema>;
