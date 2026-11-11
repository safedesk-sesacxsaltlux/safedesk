import Link from "next/link";
import Icon from "../_components/Icon";
import { AgentHeader } from "../_components/Headers";

export const metadata = { title: "상담 대기열 · SafeDesk 상담원" };

const QUEUE = [
  { no: 1, flagged: false, hl: true },
  { no: 2, flagged: true, hl: false },
  { no: 3, flagged: false, hl: false },
];

export default function AgentConsolePage() {
  return (
    <>
      <AgentHeader active="queue" />
      <main className="container-wide main">
        <div className="between">
          <h1 style={{ fontSize: 24 }}>상담 대기열</h1>
          <span className="small muted">실시간 갱신 · 마지막 갱신 HH:MM:SS</span>
        </div>

        <div className="stats" style={{ gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))" }}>
          <div className="stat"><span>대기 중 (QUEUED)</span><b style={{ color: "var(--primary)" }}>[N]</b></div>
          <div className="stat"><span>내 진행 중 (ACTIVE)</span><b>[N]</b></div>
          <div className="stat"><span>오늘 처리</span><b>[N]</b></div>
          <div className="stat"><span>오늘 자동 차단된 폭언</span><b style={{ color: "var(--warn)" }}>[N]</b></div>
        </div>

        <div className="row">
          <section className="card card-flush" style={{ flex: "999 1 560px", minWidth: 0 }}>
            <div className="table-wrap">
              <table style={{ minWidth: 640 }}>
                <thead>
                  <tr>
                    <th>순번</th>
                    <th>고객</th>
                    <th>AI 상담 요약</th>
                    <th>대기 시간</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {QUEUE.map((q) => (
                    <tr key={q.no} className={q.hl ? "hl" : undefined}>
                      <td>{q.no}</td>
                      <td>
                        [고객 ID]
                        {q.flagged && (
                          <span className="badge badge-warn" style={{ marginLeft: 6 }}>위반 이력</span>
                        )}
                      </td>
                      <td>[문의 요약]</td>
                      <td>[MM:SS]</td>
                      <td style={{ textAlign: "right", padding: "10px 16px" }}>
                        <Link href="/agent/chat" className="btn btn-primary btn-sm">수락</Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="card-foot">빈 상태: “대기 중인 상담이 없습니다.”</div>
          </section>

          <aside className="agent-side">
            <section className="card stack gap-12" style={{ padding: 20 }}>
              <h2 style={{ fontSize: 16 }}>선택한 상담 미리보기</h2>
              <span className="small muted">순번 1 · [고객 ID] · AI 상담 내역</span>
              <div className="transcript">
                <span><b>고객</b> [문의 메시지]</span>
                <span><b>AI</b> [FAQ 답변]</span>
                <span><b>고객</b> [추가 문의]</span>
                <span><b>AI</b> 답변을 찾지 못했어요 → 상담원 연결 요청</span>
              </div>
            </section>
            <div role="status" className="alert alert-danger" style={{ alignItems: "flex-start", flexWrap: "nowrap" }}>
              <span style={{ marginTop: 3 }}><Icon name="alert" size={18} /></span>
              <span>
                <b>수락 실패 토스트</b>
                <br />
                이미 다른 상담원이 수락한 상담입니다. 목록을 새로고침합니다.
              </span>
            </div>
          </aside>
        </div>
      </main>
    </>
  );
}
