import { Suspense } from "react";
import LiveChat from "./LiveChat";

export const metadata = { title: "상담 · SafeDesk" };

// 상담 번호(chatId)를 주소에서 읽으므로 화면 전체를 브라우저에서 그린다 (useSearchParams)
export default function CustomerLiveChatPage() {
  return (
    <Suspense fallback={null}>
      <LiveChat />
    </Suspense>
  );
}
