import { Suspense } from "react";
import EndedResult from "./EndedResult";

export const metadata = { title: "상담 종료 · SafeDesk" };

// 상담 번호(chatId)를 주소에서 읽으므로 결과 카드는 브라우저에서 그린다 (useSearchParams)
export default function ChatEndedPage() {
  return (
    <main className="container" style={{ maxWidth: 720, padding: "48px 24px" }}>
      <div className="row" style={{ gap: 24 }}>
        <Suspense fallback={null}>
          <EndedResult />
        </Suspense>
      </div>
    </main>
  );
}
