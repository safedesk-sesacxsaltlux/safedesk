import Link from "next/link";
import Icon from "../../_components/Icon";

export const metadata = { title: "상담 · SafeDesk" };

export default function CustomerLiveChatPage() {
  return (
    <div className="chat-page">
      <div className="chat-shell">
        <div className="chat-top">
          <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
            <div className="avatar" style={{ width: 40, height: 40 }}>
              <Icon name="user" />
            </div>
            <div className="stack">
              <span className="bold" style={{ fontSize: 16 }}>상담원 [상담원명]</span>
              <span style={{ fontSize: 12, color: "var(--ok)" }}>● 상담 중 (ACTIVE)</span>
            </div>
          </div>
          <Link href="/customer/ended" className="btn btn-sm">상담 종료</Link>
        </div>

        <div className="alert alert-warn" style={{ borderRadius: 0 }}>
          <Icon name="warn" size={18} />
          <span>
            <b>경고 1 / 2</b> · 한 번 더 폭언이 감지되면 상담이 자동 종료됩니다.
          </span>
        </div>

        <div className="chat-body">
          <div className="sys-note">
            상담원이 연결되었습니다 · 이전 AI 상담 내용이 전달되었습니다
          </div>

          <div className="msg-row">
            <div className="avatar" style={{ background: "var(--line-soft)", color: "var(--text)" }}>
              <Icon name="user" size={18} />
            </div>
            <div className="bubble">[상담원 인사 및 문의 확인 메시지]</div>
          </div>

          <div className="bubble-me">[고객 일반 메시지]</div>

          <div className="stack gap-4" style={{ alignSelf: "flex-end", maxWidth: "70%", alignItems: "flex-end" }}>
            <div className="bubble-me" style={{ maxWidth: "100%", background: "var(--danger)" }}>
              [폭언이 포함된 고객 메시지]
            </div>
            <span className="small muted" style={{ fontSize: 12 }}>
              부적절한 표현 감지 · 상담원에게는 가려진 상태로 전달됨
            </span>
          </div>

          <div className="alert alert-warn" style={{ alignItems: "flex-start", border: "1px solid #f0cfa8" }}>
            <Icon name="warn" size={20} />
            <div className="stack gap-4" style={{ flex: 1 }}>
              <b>[시스템 경고] 부적절한 표현이 감지되었습니다</b>
              <span style={{ fontSize: 13 }}>
                상담원 보호 정책에 따라 경고 1회가 부여되었습니다. 같은 상담에서
                다시 감지되면 상담이 자동 종료되고 이용이 제한됩니다.
              </span>
            </div>
          </div>

          <div className="msg-row">
            <div className="avatar" style={{ background: "var(--line-soft)", color: "var(--text)" }}>
              <Icon name="user" size={18} />
            </div>
            <div className="bubble">[상담원 응답 메시지]</div>
          </div>
        </div>

        <div className="chat-foot">
          <div className="chat-input">
            <label htmlFor="cc-input" className="sr-only">메시지 입력</label>
            <input id="cc-input" className="input" type="text" placeholder="메시지를 입력하세요" style={{ borderRadius: 10 }} />
            <button type="button" className="send" aria-label="전송">
              <Icon name="send" />
            </button>
          </div>
          <div className="between" style={{ gap: 8 }}>
            <span className="small muted" style={{ fontSize: 12 }}>
              연결 끊김 시: 상단에 “연결이 끊어졌습니다. 다시 연결 중…” 띠 표시, 입력 비활성
            </span>
            <Link href="/customer/ended" style={{ fontSize: 13 }}>
              (시연) 2차 폭언 → 자동 종료
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
