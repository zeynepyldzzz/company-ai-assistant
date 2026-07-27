import {
  ChatMessageResponseSchema,
  type ChatMessageRequest,
  type ChatMessageResponse,
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
