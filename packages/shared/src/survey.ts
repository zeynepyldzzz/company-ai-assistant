import { z } from "zod";

// C-7 (#51): GET /surveys/active, POST /surveys/{id}/responses, POST /feedback
// C-8 (#52): POST/GET /admin/surveys, PUT /admin/surveys/{id}/publish, GET /admin/surveys/{id}/results
// C-13 (#121): deadline + sabit secenek semasi + tekil oy + response-count eklendi.

export const SurveyOptionSchema = z.object({
  id: z.number(),
  optionText: z.string(),
});
export type SurveyOption = z.infer<typeof SurveyOptionSchema>;

// GET /surveys/active listesindeki tek anket (calisan tarafi).
export const SurveySchema = z.object({
  id: z.number(),
  title: z.string(),
  createdAt: z.string(),
  deadline: z.string().nullable(),
  options: z.array(SurveyOptionSchema),
});
export type Survey = z.infer<typeof SurveySchema>;

// POST /surveys/{id}/responses govdesi. C-13 (#121): serbest map yerine secilen secenegin id'si.
export const SurveyResponseRequestSchema = z.object({
  optionId: z.number(),
});
export type SurveyResponseRequest = z.infer<typeof SurveyResponseRequestSchema>;

// POST /feedback govdesi. FR-43 anonimlik: employeeId ALANI YOK (bilincli).
export const FeedbackRequestSchema = z.object({
  surveyId: z.number().nullable().optional(),
  content: z.string().min(1),
});
export type FeedbackRequest = z.infer<typeof FeedbackRequestSchema>;

// POST /admin/surveys govdesi. C-13 (#121): deadline opsiyonel, min 2 secenek zorunlu.
export const AdminSurveyCreateRequestSchema = z.object({
  title: z.string().min(1),
  deadline: z.string().nullable().optional(),
  options: z.array(z.string().min(1)).min(2),
});
export type AdminSurveyCreateRequest = z.infer<typeof AdminSurveyCreateRequestSchema>;

// PUT /admin/surveys/{id} govdesi. C-13 (#121): baslik, secenekler, deadline duzenlenir.
export const AdminSurveyUpdateRequestSchema = z.object({
  title: z.string().min(1),
  deadline: z.string().nullable().optional(),
  options: z.array(z.string().min(1)).min(2),
});
export type AdminSurveyUpdateRequest = z.infer<typeof AdminSurveyUpdateRequestSchema>;

// GET /admin/surveys (liste), POST /admin/surveys, PUT /admin/surveys/{id}/publish cevabi.
export const AdminSurveySchema = z.object({
  id: z.number(),
  title: z.string(),
  published: z.boolean(),
  createdAt: z.string(),
  deadline: z.string().nullable(),
  options: z.array(SurveyOptionSchema),
});
export type AdminSurvey = z.infer<typeof AdminSurveySchema>;

// GET /admin/surveys/{id}/results cevabi.
// answerCounts: secenek metni -> kac kisi verdigi. Admin UI bunu basit bar-chart olarak cizer.
export const SurveyResultsSchema = z.object({
  surveyId: z.number(),
  title: z.string(),
  published: z.boolean(),
  totalResponses: z.number(),
  totalFeedback: z.number(),
  answerCounts: z.record(z.string(), z.number()),
  feedbackComments: z.array(z.string()),
});
export type SurveyResults = z.infer<typeof SurveyResultsSchema>;

// GET /surveys/{id}/response-count cevabi (calisana acik, progress bar icin).
export const SurveyResponseCountSchema = z.object({
  surveyId: z.number(),
  totalResponses: z.number(),
});
export type SurveyResponseCount = z.infer<typeof SurveyResponseCountSchema>;
