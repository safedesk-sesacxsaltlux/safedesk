import { AdminHeader } from "../../_components/Headers";

export const metadata = { title: "정책 관리 · SafeDesk 관리자" };

export default function PolicyPage() {
  return (
    <>
      <AdminHeader active="policy" />
      <main className="container-wide main" style={{ maxWidth: 960 }}>
        <div className="stack gap-8">
          <h1 style={{ fontSize: 24 }}>제재 정책 관리</h1>
          <p className="muted small">
            변경한 정책은 저장 이후 시작되는 상담과 새로 발생하는 위반부터 적용됩니다.
          </p>
        </div>

        <section className="card stack gap-16">
          <h2>상담 중 처리 기준</h2>
          <div className="policy-grid">
            <div className="field" style={{ flex: "1 1 260px" }}>
              <label htmlFor="pl-warn">경고 발송 기준</label>
              <div className="inline-num">
                <span>폭언</span>
                <input id="pl-warn" className="input sm" type="number" defaultValue={1} min={1} />
                <span>회째 감지 시 경고</span>
              </div>
            </div>
            <div className="field" style={{ flex: "1 1 260px" }}>
              <label htmlFor="pl-end">자동 종료 기준</label>
              <div className="inline-num">
                <span>같은 상담 중</span>
                <input id="pl-end" className="input sm" type="number" defaultValue={2} min={2} />
                <span>회 감지 시 종료</span>
              </div>
              <span className="small muted">경고 기준보다 커야 합니다.</span>
            </div>
          </div>
        </section>

        <section className="card stack gap-16">
          <div className="stack gap-4">
            <h2>누적 위반별 이용정지</h2>
            <span className="small muted">강제 종료 1회 = 위반 1회 누적</span>
          </div>

          <div className="inline-num" style={{ borderBottom: "1px solid var(--line-soft)", paddingBottom: 14 }}>
            <span className="bold" style={{ width: 110 }}>누적 1회</span>
            <label htmlFor="pl-d1" className="muted small">정지 기간</label>
            <input id="pl-d1" className="input sm" type="number" defaultValue={7} min={1} />
            <span>일</span>
          </div>
          <div className="inline-num" style={{ borderBottom: "1px solid var(--line-soft)", paddingBottom: 14 }}>
            <span className="bold" style={{ width: 110 }}>누적 2회</span>
            <label htmlFor="pl-d2" className="muted small">정지 기간</label>
            <input id="pl-d2" className="input sm" type="number" defaultValue={30} min={1} />
            <span>일</span>
          </div>
          <div className="stack gap-8">
            <div className="inline-num">
              <span className="bold" style={{ width: 110 }}>누적 3회 이상</span>
              <label htmlFor="pl-d3" className="muted small">정지 기간</label>
              <input id="pl-d3" className="input sm err" type="number" defaultValue={0} />
              <span>일</span>
              <span style={{ display: "inline-flex", alignItems: "center", gap: 6, marginLeft: 12 }}>
                <input id="pl-rv" type="checkbox" defaultChecked style={{ width: 18, height: 18 }} />
                <label htmlFor="pl-rv" style={{ fontSize: 14 }}>관리자 검토 대상 지정</label>
              </span>
            </div>
            <span className="msg-err">정지 기간은 1일 이상이어야 합니다. (입력 검증 오류 상태)</span>
          </div>
        </section>

        <div style={{ display: "flex", gap: 10, justifyContent: "flex-end", flexWrap: "wrap" }}>
          <button type="button" className="btn">기본값으로 되돌리기</button>
          <button type="button" className="btn btn-primary" style={{ background: "var(--admin)", borderColor: "var(--admin)" }}>
            저장
          </button>
        </div>

        <section className="card stack gap-12">
          <h2>변경 이력</h2>
          <span className="small muted">YYYY.MM.DD HH:MM · [관리자명] · 누적 2회 정지 기간 변경</span>
          <span className="small muted">YYYY.MM.DD HH:MM · [관리자명] · 기본 정책 등록</span>
        </section>
      </main>
    </>
  );
}
