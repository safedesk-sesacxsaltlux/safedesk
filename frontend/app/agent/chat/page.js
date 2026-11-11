import Link from "next/link";
import Icon from "../../_components/Icon";
import { AgentHeader } from "../../_components/Headers";

export const metadata = { title: "진행 중 상담 · SafeDesk 상담원" };

export default function AgentChatPage() {
  return (
    <>
      <AgentHeader active="chat" busy />
      <main className="container-wide main" style={{ paddingTop: 20 }}>
        <div className="agent-layout">
          <section className="card agent-chat">
            <div className="chat-top">
              <div className="stack">
                <span className="bold" style={{ fontSize: 16 }}>[고객 ID] 님과 상담 중</span>
                <span className="muted" style={{ fontSize: 12 }}>
                  상담번호 [C-000000] · ACTIVE · 경과 [MM:SS]
                </span>
              </div>
              <Link href="/agent" className="btn btn-sm">상담 종료</Link>
            </div>

            <div className="chat-body">
              <div className="sys-note">상담을 수락했습니다 · HH:MM</div>

              <div className="bubble-me bubble-agent">[상담원 인사 메시지]</div>

              <div className="stack gap-4" style={{ maxWidth: "72%" }}>
                <div className="bubble">[고객 일반 메시지]</div>
                <span className="muted" style={{ fontSize: 12 }}>HH:MM</span>
              </div>

              <div className="stack gap-4" style={{ maxWidth: "72%" }}>
                <div className="bubble-masked">
                  <Icon name="shield" size={18} />
                  <span>부적절한 표현이 감지되어 가려진 메시지입니다</span>
                </div>
                <span className="muted" style={{ fontSize: 12 }}>
                  HH:MM · 원문은 관리자만 열람 가능
                </span>
              </div>

              <div className="alert alert-warn" style={{ padding: "10px 14px" }}>
                <Icon name="warn" size={18} />
                <span>
                  고객에게 <b>경고 1회</b>가 자동 발송되었습니다. 다음 감지 시
                  상담이 자동 종료됩니다.
                </span>
              </div>

              <div className="bubble-me bubble-agent">[상담원 응답 메시지]</div>

              <div className="dashed">
                <b>상태 변형 · 자동 종료 시</b> — “고객의 반복 폭언으로 상담이 자동
                종료되었습니다 (TERMINATED)” 안내 + 입력창 비활성 +{" "}
                <Link href="/agent">[대기열로 돌아가기]</Link>
              </div>
            </div>

            <div className="chat-foot">
              <div className="chip-row">
                <button type="button" className="chip">[빠른 답변 1]</button>
                <button type="button" className="chip">[빠른 답변 2]</button>
                <button type="button" className="chip">[빠른 답변 3]</button>
              </div>
              <div className="chat-input">
                <label htmlFor="ag-input" className="sr-only">답변 입력</label>
                <input id="ag-input" className="input" type="text" placeholder="답변을 입력하세요" style={{ borderRadius: 10 }} />
                <button type="button" className="btn btn-primary" style={{ fontWeight: 500, whiteSpace: "nowrap" }}>
                  전송
                </button>
              </div>
            </div>
          </section>

          <aside className="agent-side">
            <section className="card stack gap-12" style={{ padding: 20 }}>
              <h2 style={{ fontSize: 16 }}>고객 정보</h2>
              <div className="kv" style={{ padding: 0, border: 0 }}><span>고객 ID</span><span>[고객 ID]</span></div>
              <div className="kv" style={{ padding: 0, border: 0 }}><span>누적 위반</span><span>0회</span></div>
              <div className="stack gap-8">
                <div className="kv" style={{ padding: 0, border: 0 }}>
                  <span>이번 상담 경고</span>
                  <span className="bold" style={{ color: "var(--warn)" }}>1 / 2</span>
                </div>
                <div className="progress"><div style={{ width: "50%" }} /></div>
              </div>
            </section>

            <section className="card stack gap-12" style={{ padding: 20 }}>
              <h2 style={{ fontSize: 16 }}>이전 AI 상담 내역</h2>
              <div className="transcript">
                <span><b>고객</b> [문의 메시지]</span>
                <span><b>AI</b> [FAQ 답변]</span>
                <span><b>AI</b> 상담원 연결 요청됨</span>
              </div>
            </section>

            <section className="card stack gap-8" style={{ padding: 18, borderStyle: "dashed", borderColor: "#b8bec8" }}>
              <span className="small bold muted" style={{ fontSize: 12 }}>추가 기능 (Should)</span>
              <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                <button type="button" className="btn btn-sm" style={{ minHeight: 40, fontSize: 13 }}>수동 경고</button>
                <button type="button" className="btn btn-sm" style={{ minHeight: 40, fontSize: 13 }}>수동 종료</button>
              </div>
            </section>
          </aside>
        </div>
      </main>
    </>
  );
}
