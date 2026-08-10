import { z } from "zod";

// POST /chatbot/messages (A-3 backend, A-8 web UI)
// Tek atimlik soru-cevap; conversation/gecmis kavrami yok.
export const ChatMessageRequestSchema = z.object({
  message: z.string().trim().min(1).max(1000),
  // A-37 (#203): onceki yanitin intent'i — "peki yarin?" tipi takip sorulari icin.
  // Sunucu sohbet durumu TUTMAZ (chat_message_log'da kimlik yok, V13 ekip karari),
  // bu yuzden baglami istemci tasir. Opsiyonel: gonderilmezse davranis degismez.
  previousIntent: z.string().max(64).optional(),
});
export type ChatMessageRequest = z.infer<typeof ChatMessageRequestSchema>;

// A-22: yonlendirme butonu. target SEMANTIK bir hedeftir, web URL'i DEGIL
// ("directory_employees"). Istemci onu kendi navigasyonuna cevirir; Faz 2'de mobil
// ayni yaniti kullanabilsin diye URL dondurulmuyor.
export const ChatActionSchema = z.object({
  target: z.string(),
  label: z.string(),
});
export type ChatAction = z.infer<typeof ChatActionSchema>;

// A-22: tiklanabilir ornek soru. Tiklandiginda question metni normal akistan gonderilir.
export const ChatSuggestionSchema = z.object({
  label: z.string(),
  question: z.string(),
});
export type ChatSuggestion = z.infer<typeof ChatSuggestionSchema>;

// Backend ChatMessageResponse: { reply, intent, timestamp(Instant -> ISO string), actions, suggestions }
// actions/suggestions .default([]) ile: alanlar A-22'de eklendi, eski bir yanit gelirse
// (cache, eski surum) sema patlamasin — istemci her zaman dizi gorur.
export const ChatMessageResponseSchema = z.object({
  reply: z.string(),
  intent: z.string(),
  timestamp: z.string(),
  actions: z.array(ChatActionSchema).default([]),
  suggestions: z.array(ChatSuggestionSchema).default([]),
});
export type ChatMessageResponse = z.infer<typeof ChatMessageResponseSchema>;

// GET /chatbot/welcome (A-22): sohbet acilisinda gosterilen karsilama.
export const ChatWelcomeResponseSchema = z.object({
  message: z.string(),
  suggestions: z.array(ChatSuggestionSchema).default([]),
});
export type ChatWelcomeResponse = z.infer<typeof ChatWelcomeResponseSchema>;
