import { useEffect, useRef, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Link } from "react-router";
import { ArrowUpRight, Bot, Send, User } from "lucide-react";
import type { ChatAction, ChatSuggestion } from "@company/shared";
import { useAuth } from "@/auth/auth-context";
import { fetchChatWelcome, sendChatMessage } from "@/api/chat";
import { ApiError } from "@/api/client";
import { routeForAction } from "@/lib/chat-actions";
import { cn } from "@/lib/utils";

// Tek oturumluk sohbet: mesajlar yalnizca yerel state'te tutulur, panel
// unmount oldugunda kaybolur (session gecmisi kavrami yok). A-8.
type ChatMessage = {
  id: string;
  role: "user" | "assistant";
  text: string;
  error?: boolean;
  // A-22: yanitla birlikte donen yonlendirme butonlari ve (yalnizca intent
  // bulunamadiginda) ornek sorular.
  actions?: ChatAction[];
  suggestions?: ChatSuggestion[];
};

function makeId(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

/**
 * A-22: tiklanabilir ornek sorular. Tiklama metni normal akistan gonderir — kullanici
 * yazmis gibi islenir ve chat_message_log'a duser (hangi onerilerin tiklandigi
 * kalibrasyon icin veri).
 */
function SuggestionChips({
  suggestions,
  onSelect,
  disabled,
}: {
  suggestions: ChatSuggestion[];
  onSelect: (question: string) => void;
  disabled: boolean;
}) {
  if (suggestions.length === 0) return null;
  return (
    <div className="flex flex-wrap gap-2">
      {suggestions.map((s) => (
        <button
          key={s.question}
          type="button"
          disabled={disabled}
          onClick={() => onSelect(s.question)}
          className="bg-primary-soft text-primary hover:bg-primary/15 rounded-full px-3 py-1.5 text-sm font-medium transition-colors disabled:pointer-events-none disabled:opacity-50"
        >
          {s.label}
        </button>
      ))}
    </div>
  );
}

/**
 * A-22: chatbot'un sinirinin otesine kopru. Bilinmeyen hedefte buton gizlenir
 * (bkz. routeForAction) — kirik link gostermektense hic gostermemek dogru.
 */
function ActionButtons({ actions }: { actions: ChatAction[] }) {
  const routable = actions
    .map((action) => ({ action, to: routeForAction(action.target) }))
    .filter((item): item is { action: ChatAction; to: string } => item.to !== null);

  if (routable.length === 0) return null;
  return (
    <div className="flex flex-wrap gap-2">
      {routable.map(({ action, to }) => (
        <Link
          key={action.target}
          to={to}
          className="border-primary/40 text-primary hover:bg-primary-soft inline-flex items-center gap-1.5 rounded-md border px-3 py-1.5 text-sm font-medium transition-colors"
        >
          {action.label}
          <ArrowUpRight className="size-4" />
        </Link>
      ))}
    </div>
  );
}

function MessageBubble({
  message,
  onSelectSuggestion,
  disabled,
}: {
  message: ChatMessage;
  onSelectSuggestion: (question: string) => void;
  disabled: boolean;
}) {
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
      <div className={cn("flex max-w-[88%] flex-col gap-2", isUser && "items-end")}>
        <div
          className={cn(
            "rounded-lg px-3 py-2 text-sm whitespace-pre-wrap",
            isUser
              ? "bg-primary text-primary-foreground"
              : message.error
                ? "bg-destructive/10 text-destructive"
                : "bg-muted"
          )}
        >
          {message.text}
        </div>
        {message.actions && message.actions.length > 0 && <ActionButtons actions={message.actions} />}
        {message.suggestions && message.suggestions.length > 0 && (
          <SuggestionChips
            suggestions={message.suggestions}
            onSelect={onSelectSuggestion}
            disabled={disabled}
          />
        )}
      </div>
    </div>
  );
}

/**
 * A-22: sohbet bosken gosterilen karsilama. Ilk mesajdan sonra kaybolur — ekrani
 * doldurmasin. Metin ve oneriler backend'den gelir (selamlama template'i + intent
 * tablosu), burada sabit liste yok.
 */
function WelcomeBlock({
  message,
  suggestions,
  onSelect,
  disabled,
}: {
  message: string;
  suggestions: ChatSuggestion[];
  onSelect: (question: string) => void;
  disabled: boolean;
}) {
  return (
    <div className="flex gap-3">
      <div className="bg-muted text-muted-foreground flex size-8 shrink-0 items-center justify-center rounded-full">
        <Bot className="size-4" />
      </div>
      <div className="flex max-w-[88%] flex-col gap-3">
        <div className="bg-muted rounded-lg px-3 py-2 text-sm whitespace-pre-wrap">{message}</div>
        <SuggestionChips suggestions={suggestions} onSelect={onSelect} disabled={disabled} />
      </div>
    </div>
  );
}

export function ChatPanel({
  emptyHint = "Şirketle ilgili bir soru sorun; asistan yanıtlasın.",
}: {
  emptyHint?: string;
}) {
  const { token } = useAuth();
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const scrollRef = useRef<HTMLDivElement>(null);

  // A-37 (#203): son yanitin intent'i. "peki yarın?" gibi tek basina anlamsiz bir mesaj
  // geldiginde sunucu bunu kullanarak onceki konuyu surdurur.
  //
  // Sunucuda TUTULMUYOR: chat_message_log'da kimlik yok (V13 ekip karari) ve sunucu
  // tarafinda sohbet gecmisi tutmak o karari bozardi. Istemci zaten gecmisi bellekte
  // tuttugu icin tasimasi bedava.
  //
  // state yerine ref: degeri yalnizca bir sonraki istekte okuyoruz, degismesi yeniden
  // render gerektirmiyor.
  const lastIntentRef = useRef<string | null>(null);

  const welcomeQuery = useQuery({
    queryKey: ["chat", "welcome"],
    queryFn: () => fetchChatWelcome(token!),
    enabled: Boolean(token),
    // Karsilama icerigi migration ile degisir, oturum icinde degil.
    staleTime: Infinity,
  });

  const mutation = useMutation({
    mutationFn: (message: string) =>
      sendChatMessage({ message, previousIntent: lastIntentRef.current ?? undefined }, token!),
    onSuccess: (data) => {
      lastIntentRef.current = data.intent;
      setMessages((prev) => [
        ...prev,
        {
          id: makeId(),
          role: "assistant",
          text: data.reply,
          actions: data.actions,
          suggestions: data.suggestions,
        },
      ]);
    },
    onError: (err) => {
      // Hata durumunda baglam TEMIZLENIR: kullanicinin bir sonraki mesaji, cevabini hic
      // alamadigi bir konuya baglanmamali.
      lastIntentRef.current = null;
      const text =
        err instanceof ApiError ? err.message : "Yanıt alınamadı, lütfen tekrar deneyin.";
      setMessages((prev) => [...prev, { id: makeId(), role: "assistant", text, error: true }]);
    },
  });

  // Her yeni mesaj / typing durumunda en alta kaydir.
  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: "smooth" });
  }, [messages, mutation.isPending]);

  function sendMessage(text: string) {
    const trimmed = text.trim();
    if (!trimmed || mutation.isPending) return;
    setMessages((prev) => [...prev, { id: makeId(), role: "user", text: trimmed }]);
    setInput("");
    mutation.mutate(trimmed);
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    sendMessage(input);
  }

  return (
    <div className="flex h-full min-h-0 flex-col">
      <div ref={scrollRef} className="flex-1 space-y-4 overflow-auto pr-1">
        {/* Karsilama sohbetin ILK MESAJI olarak kalir, ilk soruda silinmez: liste zaten
            kaydirilabilir oldugu icin asagiyi itmiyor, bunun karsiliginda sohbet akisi
            tutarli kaliyor ve oneri chip'leri yukari kaydirilarak tekrar kullanilabiliyor. */}
        {welcomeQuery.data && (
          <WelcomeBlock
            message={welcomeQuery.data.message}
            suggestions={welcomeQuery.data.suggestions}
            onSelect={sendMessage}
            disabled={mutation.isPending}
          />
        )}
        {/* Karsilama gelemezse (ag hatasi) sohbet calismaya devam etmeli: eski ipucu metnine dusulur. */}
        {!welcomeQuery.data && !welcomeQuery.isLoading && messages.length === 0 && (
          <p className="text-muted-foreground text-sm">{emptyHint}</p>
        )}
        {messages.map((m) => (
          <MessageBubble
            key={m.id}
            message={m}
            onSelectSuggestion={sendMessage}
            disabled={mutation.isPending}
          />
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

function TypingIndicator() {
  return (
    <div className="flex gap-3">
      <div className="bg-muted text-muted-foreground flex size-8 shrink-0 items-center justify-center rounded-full">
        <Bot className="size-4" />
      </div>
      <div
        className="bg-muted flex items-center gap-1 rounded-lg px-3 py-3"
        aria-label="Yanıt yazılıyor"
      >
        <span className="bg-muted-foreground size-1.5 animate-bounce rounded-full [animation-delay:-0.3s]" />
        <span className="bg-muted-foreground size-1.5 animate-bounce rounded-full [animation-delay:-0.15s]" />
        <span className="bg-muted-foreground size-1.5 animate-bounce rounded-full" />
      </div>
    </div>
  );
}
