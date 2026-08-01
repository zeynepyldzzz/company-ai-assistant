import { useState } from "react";
import { useLocation } from "react-router";
import { MessageCircle, X } from "lucide-react";
import { ChatPanel } from "@/components/chat-panel";
import { cn } from "@/lib/utils";

export function FloatingChatBubble() {
  const [open, setOpen] = useState(false);
  const { pathname } = useLocation();

  // Tam sayfa Asistan ekranindayken bubble'i gizle, ayni sohbeti iki yerde
  // ayni anda gostermemek icin.
  if (pathname === "/chat") return null;

  return (
    <>
      <div
        className={cn(
          "bg-card fixed right-6 bottom-24 z-50 flex h-[600px] max-h-[calc(100vh-8rem)] w-[420px] max-w-[calc(100vw-2rem)] origin-bottom-right flex-col rounded-2xl border p-4 shadow-2xl transition-all duration-[220ms] ease-out",
          open ? "translate-y-0 scale-100 opacity-100" : "pointer-events-none translate-y-2 scale-95 opacity-0"
        )}
      >
        <div className="mb-3 flex items-center justify-between">
          <span className="text-sm font-semibold">Asistan</span>
          <button
            type="button"
            onClick={() => setOpen(false)}
            aria-label="Sohbeti kapat"
            className="text-muted-foreground hover:text-foreground rounded-md p-1"
          >
            <X className="size-4" />
          </button>
        </div>
        <ChatPanel emptyHint="Hızlı bir soru sorun." />
      </div>

      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-label={open ? "Sohbeti kapat" : "Asistanı aç"}
        className="bg-primary text-primary-foreground hover:bg-primary/90 fixed right-6 bottom-6 z-50 flex size-14 items-center justify-center rounded-full shadow-lg transition-transform hover:scale-105"
      >
        {open ? <X className="size-6" /> : <MessageCircle className="size-6" />}
      </button>
    </>
  );
}
