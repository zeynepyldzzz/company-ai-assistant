import {
  ChatMessageResponseSchema,
  ChatWelcomeResponseSchema,
  type ChatMessageRequest,
  type ChatMessageResponse,
  type ChatWelcomeResponse,
} from "@company/shared";
import { apiFetch } from "./client";

// POST /chatbot/messages -> tek yanit (reply, intent, timestamp).
// Gecmis tutulmaz; her cagri bagimsizdir.
export async function sendChatMessage(
  body: ChatMessageRequest,
  token: string
): Promise<ChatMessageResponse> {
  const data = await apiFetch<unknown>("/chatbot/messages", {
    method: "POST",
    token,
    body: JSON.stringify(body),
  });
  return ChatMessageResponseSchema.parse(data);
}

// GET /chatbot/welcome -> karsilama metni + tiklanabilir ornek sorular (A-22).
// Icerik backend'de intent tablosundan turetilir; burada sabit liste TUTULMAZ.
export async function fetchChatWelcome(token: string): Promise<ChatWelcomeResponse> {
  const data = await apiFetch<unknown>("/chatbot/welcome", { token });
  return ChatWelcomeResponseSchema.parse(data);
}
