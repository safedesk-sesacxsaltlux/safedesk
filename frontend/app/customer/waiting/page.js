import { Suspense } from "react";
import Link from "next/link";
import Icon from "../../_components/Icon";
import CancelWaitButton from "./CancelWaitButton";
import WaitForAgent from "./WaitForAgent";

export const metadata = { title: "상담원 연결 대기 · SafeDesk" };

export default function WaitingPage() {
  return (
    <div className="chat-page">
      <div className="chat-shell">
        <div className="chat-top">
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <Link href="/customer" aria-label="뒤로" className="icon-link">
              <Icon name="back" />
            </Link>
            <div className="stack">
              <span className="bold" style={{ fontSize: 16 }}>상담원 연결 중</span>
              <span className="muted" style={{ fontSize: 12 }}>대기 중</span>
            </div>
          </div>
        </div>

        <div className="wait-body">
          <div className="spinner" />
          <div className="stack gap-8">
            <h1 style={{ fontSize: 24 }}>상담원을 연결하고 있어요</h1>
            <p className="muted" style={{ fontSize: 15 }}>화면을 닫아도 대기 순서는 유지됩니다.</p>
          </div>
          <div className="alert alert-warn" style={{ maxWidth: 460, textAlign: "left" }}>
            상담원도 누군가의 가족입니다. 상담 중 욕설·폭언이 감지되면 경고 후
            상담이 자동 종료되며, 이용이 제한될 수 있습니다.
          </div>
          <div style={{ display: "flex", gap: 10, flexWrap: "wrap", justifyContent: "center" }}>
            {/* 주소의 chatId 를 읽는 버튼만 브라우저에서 그린다 (useSearchParams) */}
            <Suspense fallback={null}>
              <CancelWaitButton />
              <WaitForAgent />
            </Suspense>
          </div>
        </div>
      </div>
    </div>
  );
}
