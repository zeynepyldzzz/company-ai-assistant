import { useEffect, useRef, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Bot, Send, User } from "lucide-react";
import { useAuth } from "@/auth/auth-context";
import { sendChatMessage } from "@/api/chat";
import { ApiError } from "@/api/client";
import { cn } from "@/lib/utils";

// Tek oturumluk sohbet: mesajlar yalnizca yerel state'te tutulur, panel
// unmount oldugunda kaybolur (session gecmisi kavrami yok). A-8.
type ChatMessage = {
  id: string;
  role: "user" | "assistant";
  text: string;
  error?: boolean;
};

function makeId(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

function MessageBubble({ message }: { message: ChatMessage }) {
  const isUser = message.role === "user";
  return (
    <div className={cn("flex gap-3", isUser && "flex-row-reverse")}>
      <div
        className={cn(
          "flex size-8 shrink-0 items-center justify-center rounded-full",
          isUser ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground"
        )}
      >
        {isUser ? <User className="size-4" /> : <Bot className="size-4" />}
      </div>
      <div
        className={cn(
          "max-w-[80%] rounded-lg px-3 py-2 text-sm whitespace-pre-wrap",
          isUser
            ? "bg-primary text-primary-foreground"
            : message.error
              ? "bg-destructive/10 text-destructive"
              : "bg-muted"
        )}
      >
        {message.text}
      </div>
    </div>
  );
}

function TypingIndicator() {
  return (
    <div className="flex gap-3">
      <div className="bg-muted text-muted-foreground flex size-8 shrink-0 items-center justify-center rounded-full">
        <Bot className="size-4" />
      </div>
      <div className="bg-muted flex items-center gap-1 rounded-lg px-3 py-3" aria-label="Yanıt yazılıyor">
        <span className="bg-muted-foreground size-1.5 animate-bounce rounded-full [animation-delay:-0.3s]" />
        <span className="bg-muted-foreground size-1.5 animate-bounce rounded-full [animation-delay:-0.15s]" />
        <span className="bg-muted-foreground size-1.5 animate-bounce rounded-full" />
      </div>
    </div>
  );
}

export function ChatPanel({ emptyHint = "Şirketle ilgili bir soru sorun; asistan yanıtlasın." }: { emptyHint?: string }) {
  const { token } = useAuth();
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const scrollRef = useRef<HTMLDivElement>(null);

  const mutation = useMutation({
    mutationFn: (message: string) => sendChatMessage({ message }, token!),
    onSuccess: (data) => {
      setMessages((prev) => [...prev, { id: makeId(), role: "assistant", text: data.reply }]);
    },
    onError: (err) => {
      const text =
        err instanceof ApiError ? err.message : "Yanıt alınamadı, lütfen tekrar deneyin.";
      setMessages((prev) => [...prev, { id: makeId(), role: "assistant", text, error: true }]);
    },
  });

  // Her yeni mesaj / typing durumunda en alta kaydir.
  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: "smooth" });
  }, [messages, mutation.isPending]);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const trimmed = input.trim();
    if (!trimmed || mutation.isPending) return;
    setMessages((prev) => [...prev, { id: makeId(), role: "user", text: trimmed }]);
    setInput("");
    mutation.mutate(trimmed);
  }

  return (
    <div className="flex h-full min-h-0 flex-col">
      <div ref={scrollRef} className="flex-1 space-y-4 overflow-auto pr-1">
        {messages.length === 0 && !mutation.isPending && (
          <p className="text-muted-foreground text-sm">{emptyHint}</p>
        )}
        {messages.map((m) => (
          <MessageBubble key={m.id} message={m} />
        ))}
        {mutation.isPending && <TypingIndicator />}
      </div>

      <form onSubmit={handleSubmit} className="mt-4 flex gap-2">
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          maxLength={1000}
          placeholder="Mesajınızı yazın…"
          className="border-input bg-background focus-visible:ring-ring flex-1 rounded-md border px-3 py-2 text-sm focus-visible:ring-2 focus-visible:outline-none"
        />
        <button
          type="submit"
          disabled={!input.trim() || mutation.isPending}
          className="bg-primary text-primary-foreground hover:bg-primary/90 inline-flex items-center gap-2 rounded-md px-4 py-2 text-sm font-medium disabled:pointer-events-none disabled:opacity-50"
        >
          <Send className="size-4" />
          Gönder
        </button>
      </form>
    </div>
  );
}
