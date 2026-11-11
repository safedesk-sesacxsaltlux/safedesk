import Link from "next/link";
import { AdminHeader } from "../../../_components/Headers";

export const metadata = { title: "제재 상세 · SafeDesk 관리자" };

// UI 학습용: id 는 아직 사용하지 않고 같은 화면을 보여줍니다.
export default function SanctionDetailPage() {
  return (
    <>
      <AdminHeader active="sanctions" />
      <main className="container-wide main">
        <div className="breadcrumb">
          <Link href="/admin/sanctions">제재 관리</Link> › [고객 ID]
        </div>

        <div className="row">
          <section className="card stack gap-12" style={{ flex: "1 1 300px" }}>
            <div className="between" style={{ flexWrap: "nowrap" }}>
              <h1 style={{ fontSize: 22 }}>[고객 ID]</h1>
              <span className="badge badge-danger">정지 중</span>
            </div>
            <div className="kv"><span>누적 위반</span><b>2회</b></div>
            <div className="kv"><span>현재 제재</span><span>30일 이용정지</span></div>
            <div className="kv"><span>기간</span><span>YYYY.MM.DD ~ YYYY.MM.DD</span></div>
            <div className="kv"><span>가입일</span><span>YYYY.MM.DD</span></div>
            <button type="button" className="btn btn-danger">제재 해제</button>
            <span className="small muted">해제해도 위반 이력은 보존됩니다.</span>
          </section>

          <div className="stack gap-20" style={{ flex: "2 1 520px", minWidth: 0 }}>
            <section className="card stack gap-12">
              <h2>위반 이력</h2>
              <div className="timeline">
                <div className="tl-item stack gap-4">
                  <span className="muted small">YYYY.MM.DD</span>
                  <span>
                    위반 2회차 · 상담 <Link href="/admin/history/1">[C-000000]</Link> 강제 종료 (TERMINATED)
                  </span>
                  <span className="small" style={{ color: "var(--danger)" }}>30일 정지 적용</span>
                </div>
                <div className="tl-item stack gap-4">
                  <span className="muted small">YYYY.MM.DD</span>
                  <span>
                    위반 1회차 · 상담 <Link href="/admin/history/1">[C-000000]</Link> 강제 종료 (TERMINATED)
                  </span>
                  <span className="small muted">7일 정지 적용 (만료)</span>
                </div>
              </div>
            </section>

            <section className="card stack gap-12">
              <div className="between">
                <h2>폭언 원문 (관리자 전용)</h2>
                <span className="small muted">열람 시 관리자 ID와 시각이 기록됩니다</span>
              </div>
              <div className="timeline">
                <div className="tl-item stack gap-4" style={{ background: "var(--danger-soft)", borderColor: "#f2c4be" }}>
                  <span className="small muted">HH:MM · 경고 1</span>
                  <span>[폭언 원문 텍스트 — 열람 상태]</span>
                  <span className="badge badge-danger" style={{ alignSelf: "flex-start" }}>ABUSIVE</span>
                </div>
                <div className="tl-item stack gap-8">
                  <span className="small muted">HH:MM · 경고 2</span>
                  <span>●●●●●●●● (가려짐)</span>
                  <button type="button" className="btn btn-sm" style={{ alignSelf: "flex-start" }}>
                    원문 보기
                  </button>
                </div>
              </div>
            </section>
          </div>
        </div>

        <section className="modal-demo" aria-label="제재 해제 확인 모달 예시">
          <div className="modal-box">
            <h3 style={{ fontSize: 18 }}>제재를 해제할까요?</h3>
            <p className="muted" style={{ fontSize: 14, lineHeight: 1.6 }}>
              [고객 ID]의 30일 이용정지가 즉시 해제되고, 고객은 바로 상담을 시작할 수
              있습니다. 위반 이력은 보존됩니다.
            </p>
            <div className="field">
              <label htmlFor="sd-reason">해제 사유 (필수)</label>
              <textarea id="sd-reason" className="input" rows={3} placeholder="해제 사유를 입력하세요" />
            </div>
            <div style={{ display: "flex", gap: 8, justifyContent: "flex-end" }}>
              <button type="button" className="btn">취소</button>
              <button type="button" className="btn btn-danger">해제하기</button>
            </div>
          </div>
        </section>
      </main>
    </>
  );
}
