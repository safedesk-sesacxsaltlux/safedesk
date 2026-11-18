import { Suspense } from "react";
import ChatRoom from "./ChatRoom";

export const metadata = { title: "AI 상담 · SafeDesk" };

// 상담 번호(chatId)를 주소에서 읽으므로 화면 전체를 브라우저에서 그린다 (useSearchParams)
export default function AiChatPage() {
  return (
    <Suspense fallback={null}>
      <ChatRoom />
    </Suspense>
  );
}
