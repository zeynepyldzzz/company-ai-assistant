import { ChatPanel } from "@/components/chat-panel";

export function ChatPage() {
  return (
    <div className="flex h-full flex-col">
      <h1 className="mb-4 text-xl font-semibold">Asistan</h1>
      <ChatPanel />
    </div>
  );
}
