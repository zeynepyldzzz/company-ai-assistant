import { ChatPanel } from "@/components/chat-panel";

// Sohbet, diger sayfalardan farkli olarak ekrani DOLDURMALI: mesaj listesi kendi icinde
// kayiyor, sayfa kaymiyor. AppLayout'un alt bosluguna (pb-12) burada ihtiyac yok — form
// zaten en altta duruyor — negatif margin ile geri aliniyor.
export function ChatPage() {
  return (
    <div className="-mb-8 flex h-full flex-col">
      <h1 className="mb-3 text-xl font-semibold">Asistan</h1>
      <ChatPanel />
    </div>
  );
}
