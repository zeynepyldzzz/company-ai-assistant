import { z } from "zod";

// POST /chatbot/messages (A-3 backend, A-8 web UI)
// Tek atimlik soru-cevap; conversation/gecmis kavrami yok.
export const ChatMessageRequestSchema = z.object({
  message: z.string().trim().min(1).max(1000),
});
export type ChatMessageRequest = z.infer<typeof ChatMessageRequestSchema>;

// Backend ChatMessageResponse: { reply, intent, timestamp(Instant -> ISO string) }
export const ChatMessageResponseSchema = z.object({
  reply: z.string(),
  intent: z.string(),
  timestamp: z.string(),
});
export type ChatMessageResponse = z.infer<typeof ChatMessageResponseSchema>;
