import { z } from "zod";

// C-7 (#51): GET /surveys/active, POST /surveys/{id}/responses, POST /feedback
// C-8 (#52): POST/GET /admin/surveys, PUT /admin/surveys/{id}/publish, GET /admin/surveys/{id}/results

// GET /surveys/active listesindeki tek anket (calisan tarafi).
export const SurveySchema = z.object({
  id: z.number(),
  title: z.string(),
  createdAt: z.string(),
});
export type Survey = z.infer<typeof SurveySchema>;

// POST /surveys/{id}/responses govdesi. answers serbest form (soru -> cevap).
export const SurveyResponseRequestSchema = z.object({
  answers: z.record(z.string(), z.unknown()),
});
export type SurveyResponseRequest = z.infer<typeof SurveyResponseRequestSchema>;

// POST /feedback govdesi. FR-43 anonimlik: employeeId ALANI YOK (bilincli).
export const FeedbackRequestSchema = z.object({
  surveyId: z.number().nullable().optional(),
  content: z.string().min(1),
});
export type FeedbackRequest = z.infer<typeof FeedbackRequestSchema>;

// POST /admin/surveys govdesi.
export const AdminSurveyCreateRequestSchema = z.object({
  title: z.string().min(1),
});
export type AdminSurveyCreateRequest = z.infer<typeof AdminSurveyCreateRequestSchema>;

// GET /admin/surveys (liste), POST /admin/surveys, PUT /admin/surveys/{id}/publish cevabi.
export const AdminSurveySchema = z.object({
  id: z.number(),
  title: z.string(),
  published: z.boolean(),
  createdAt: z.string(),
});
export type AdminSurvey = z.infer<typeof AdminSurveySchema>;

// GET /admin/surveys/{id}/results cevabi.
// answerCounts: soru -> (cevap degeri -> kac kisi verdigi). Admin UI bunu
// basit bar-chart olarak cizer (ekstra bir grafik kutuphanesine gerek yok).
export const SurveyResultsSchema = z.object({
  surveyId: z.number(),
  title: z.string(),
  published: z.boolean(),
  totalResponses: z.number(),
  totalFeedback: z.number(),
  answerCounts: z.record(z.string(), z.record(z.string(), z.number())),
  feedbackComments: z.array(z.string()),
});
export type SurveyResults = z.infer<typeof SurveyResultsSchema>;
