import Link from "next/link";
import Icon from "../../_components/Icon";
import { CustomerHeader } from "../../_components/Headers";

export const metadata = { title: "내 상담 이력 · SafeDesk" };

export default function MyHistoryPage() {
  return (
    <>
      <CustomerHeader active="history" />
      <main className="container main" style={{ maxWidth: 860, paddingTop: 36 }}>
        <div className="stack gap-8">
          <h1 style={{ fontSize: 26 }}>내 상담 이력</h1>
          <p className="muted small">본인의 상담 기록만 표시됩니다.</p>
        </div>

        <div className="chip-row">
          <button type="button" className="chip on">전체</button>
          <button type="button" className="chip">AI 응대</button>
          <button type="button" className="chip">상담원 연결</button>
        </div>

        {/* details/summary 로 펼치기 · 접기 (JS 없이 동작) */}
        <details className="card accordion" open>
          <summary>
            <div className="stack gap-4">
              <span className="small muted">YYYY.MM.DD HH:MM · 상담원 연결</span>
              <span className="bold">[문의 요약 제목]</span>
            </div>
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <span className="badge badge-ok">상담 완료</span>
              <Icon name="up" size={18} />
            </div>
          </summary>
          <div className="acc-body">
            <div className="transcript">
              <span className="bold">상담 요약</span>
              <span className="muted">[문의 유형 / 처리 내용 / 처리 결과 — 규칙 기반 템플릿]</span>
            </div>
            <div className="transcript">
              <span><b>나</b> [문의 메시지]</span>
              <span><b>AI</b> [FAQ 답변]</span>
              <span><b>상담원</b> [상담원 메시지]</span>
              <span className="muted">… 대화 전체 보기</span>
            </div>
          </div>
        </details>

        <details className="card accordion">
          <summary>
            <div className="stack gap-4">
              <span className="small muted">YYYY.MM.DD HH:MM · 상담원 연결</span>
              <span className="bold">[문의 요약 제목]</span>
              <span className="small" style={{ color: "var(--danger)" }}>
                상담 중 부적절한 표현으로 종료됨 · 위반 1회 누적
              </span>
            </div>
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <span className="badge badge-danger">강제 종료</span>
              <Icon name="down" size={18} />
            </div>
          </summary>
          <div className="acc-body">
            <div className="transcript">
              <span className="bold">상담 요약</span>
              <span className="muted">[강제 종료 사유와 처리 결과 요약]</span>
            </div>
          </div>
        </details>

        <details className="card accordion">
          <summary>
            <div className="stack gap-4">
              <span className="small muted">YYYY.MM.DD HH:MM · AI 응대</span>
              <span className="bold">[문의 요약 제목]</span>
            </div>
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <span className="badge">AI 응대 완료</span>
              <Icon name="down" size={18} />
            </div>
          </summary>
          <div className="acc-body">
            <div className="transcript">
              <span><b>나</b> [문의 메시지]</span>
              <span><b>AI</b> [FAQ 답변]</span>
            </div>
          </div>
        </details>

        <button type="button" className="btn" style={{ alignSelf: "center" }}>더 보기</button>

        <div className="dashed" style={{ textAlign: "center" }}>
          빈 상태: “아직 상담 이력이 없어요.”{" "}
          <Link href="/customer/chat">[상담 시작하기]</Link>
        </div>
      </main>
    </>
  );
}
