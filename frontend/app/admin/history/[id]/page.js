import Link from "next/link";
import { AdminHeader } from "../../../_components/Headers";

export const metadata = { title: "상담 상세 · SafeDesk 관리자" };

// UI 학습용: id 는 아직 사용하지 않고 같은 화면을 보여줍니다.
export default function ConsultDetailPage() {
  return (
    <>
      <AdminHeader active="history" />
      <main className="container-wide main">
        <div className="breadcrumb">
          <Link href="/admin/history">상담 이력</Link> › [C-000000]
        </div>

        <div className="row">
          <div className="stack gap-20" style={{ flex: "1 1 340px" }}>
            <section className="card stack gap-8">
              <div className="between" style={{ flexWrap: "nowrap" }}>
                <h1 style={{ fontSize: 20 }}>[C-000000]</h1>
                <span className="badge badge-danger">TERMINATED</span>
              </div>
              <div className="kv">
                <span>고객</span>
                <Link href="/admin/sanctions/1">[고객 ID]</Link>
              </div>
              <div className="kv"><span>상담원</span><span>[상담원명]</span></div>
              <div className="kv"><span>시작</span><span>YYYY.MM.DD HH:MM</span></div>
              <div className="kv"><span>종료</span><span>YYYY.MM.DD HH:MM</span></div>
              <div className="kv"><span>종료 사유</span><span>반복 폭언 자동 종료</span></div>
              <div className="kv"><span>폭언 감지</span><span>2회</span></div>
            </section>

            <section className="card stack gap-12">
              <div className="stack gap-4">
                <h2>상담 요약</h2>
                <span className="small muted">규칙 기반 템플릿 자동 생성</span>
              </div>
              <div className="transcript">
                <div><b>문의 유형</b><br /><span className="muted">[FAQ 카테고리]</span></div>
                <div><b>처리 내용</b><br /><span className="muted">[AI 응대 → 상담원 연결 → 응대 내용]</span></div>
                <div><b>처리 결과</b><br /><span className="muted">[해결 / 미해결 / 강제 종료]</span></div>
              </div>
            </section>
          </div>

          <section className="card stack gap-12" style={{ flex: "2 1 520px", minWidth: 0 }}>
            <div className="between">
              <h2>메시지 내역</h2>
              <span className="small muted">
                폭언 원문은 이 화면에 표시하지 않습니다 ·{" "}
                <Link href="/admin/sanctions/1">제재 상세에서 열람</Link>
              </span>
            </div>
            <div className="timeline">
              <div className="tl-divider">AI 상담 구간 (BOT)</div>
              <div className="tl-item"><b>고객</b> · HH:MM<br />[문의 메시지]</div>
              <div className="tl-item"><b>AI</b> · HH:MM<br />[FAQ 답변]</div>
              <div className="tl-divider">상담원 연결 (QUEUED → ACTIVE)</div>
              <div className="tl-item"><b>상담원</b> · HH:MM<br />[상담원 메시지]</div>
              <div className="tl-item" style={{ background: "var(--warn-soft)", borderColor: "#f0cfa8" }}>
                <b>고객</b> · HH:MM <span className="badge badge-warn">ABUSIVE · 경고 1</span>
                <br /><span className="muted">[마스킹된 메시지]</span>
              </div>
              <div className="tl-item"><b>상담원</b> · HH:MM<br />[상담원 메시지]</div>
              <div className="tl-item" style={{ background: "var(--danger-soft)", borderColor: "#f2c4be" }}>
                <b>고객</b> · HH:MM <span className="badge badge-danger">ABUSIVE · 경고 2</span>
                <br /><span className="muted">[마스킹된 메시지]</span>
              </div>
              <div className="tl-divider" style={{ background: "var(--danger-soft)", color: "var(--danger)" }}>
                상담 자동 종료 (TERMINATED) · 위반 1회 누적
              </div>
            </div>
          </section>
        </div>
      </main>
    </>
  );
}
