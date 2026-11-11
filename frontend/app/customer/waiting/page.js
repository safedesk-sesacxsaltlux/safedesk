import Link from "next/link";
import Icon from "../../_components/Icon";

export const metadata = { title: "상담원 연결 대기 · SafeDesk" };

export default function WaitingPage() {
  return (
    <div className="chat-page">
      <div className="chat-shell">
        <div className="chat-top">
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <Link href="/customer/chat" aria-label="뒤로" className="icon-link">
              <Icon name="back" />
            </Link>
            <div className="stack">
              <span className="bold" style={{ fontSize: 16 }}>상담원 연결 중</span>
              <span className="muted" style={{ fontSize: 12 }}>상태 QUEUED</span>
            </div>
          </div>
        </div>

        <div className="wait-body">
          <div className="spinner" />
          <div className="stack gap-8">
            <h1 style={{ fontSize: 24 }}>상담원을 연결하고 있어요</h1>
            <p className="muted" style={{ fontSize: 15 }}>화면을 닫아도 대기 순서는 유지됩니다.</p>
          </div>
          <div style={{ display: "flex", gap: 12, flexWrap: "wrap", justifyContent: "center" }}>
            <div className="wait-stat">
              <span>내 대기 순번</span>
              <b>[N]번째</b>
            </div>
            <div className="wait-stat">
              <span>예상 대기 시간</span>
              <b>약 [N]분</b>
            </div>
          </div>
          <div className="alert alert-warn" style={{ maxWidth: 460, textAlign: "left" }}>
            상담원도 누군가의 가족입니다. 상담 중 욕설·폭언이 감지되면 경고 후
            상담이 자동 종료되며, 이용이 제한될 수 있습니다.
          </div>
          <div style={{ display: "flex", gap: 10, flexWrap: "wrap", justifyContent: "center" }}>
            <Link href="/customer" className="btn">연결 취소</Link>
            <Link href="/customer/live" className="btn btn-primary" style={{ fontWeight: 500 }}>
              (시연) 상담원 수락됨 → 채팅
            </Link>
          </div>
        </div>

        <div className="chat-foot" style={{ gap: 6 }}>
          <span className="small bold muted" style={{ fontSize: 12 }}>예외 상태</span>
          <span className="small muted">
            상담원 부재: “지금은 연결 가능한 상담원이 없습니다. 잠시 후 다시 시도해
            주세요.” + [다시 시도] [AI 상담으로 돌아가기]
          </span>
        </div>
      </div>
    </div>
  );
}
