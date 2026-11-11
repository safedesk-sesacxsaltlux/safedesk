import Link from "next/link";
import Icon from "../_components/Icon";
import { AdminHeader } from "../_components/Headers";

export const metadata = { title: "운영 현황 · SafeDesk 관리자" };

const ENDINGS = ["AI 응대 종료", "정상 종료 (CLOSED)", "강제 종료 (TERMINATED)"];

export default function AdminDashboardPage() {
  return (
    <>
      <AdminHeader active="dashboard" />
      <main className="container-wide main">
        <div className="between">
          <h1 style={{ fontSize: 24 }}>운영 현황 · 통계</h1>
          <div style={{ display: "flex", gap: 4, padding: 4, borderRadius: 10, background: "#fff", border: "1px solid var(--line)" }}>
            <button type="button" className="chip on" style={{ borderRadius: 8, minHeight: 40, fontSize: 14 }}>오늘</button>
            <button type="button" className="chip" style={{ borderRadius: 8, minHeight: 40, fontSize: 14, border: 0 }}>7일</button>
            <button type="button" className="chip" style={{ borderRadius: 8, minHeight: 40, fontSize: 14, border: 0 }}>30일</button>
          </div>
        </div>

        <div className="alert alert-danger">
          <Icon name="alert" size={18} />
          <span style={{ flex: "1 1 300px" }}>
            누적 위반 3회 이상으로 <b>관리자 검토가 필요한 고객 [N]명</b>이 있습니다.
          </span>
          <Link href="/admin/sanctions" style={{ fontWeight: 500, color: "var(--danger)" }}>
            검토하러 가기
          </Link>
        </div>

        <div className="stats">
          <div className="stat"><span>전체 상담</span><b>[N]</b></div>
          <div className="stat"><span>AI 단독 해결</span><b>[N]%</b></div>
          <div className="stat"><span>폭언 감지</span><b style={{ color: "var(--warn)" }}>[N]</b></div>
          <div className="stat"><span>자동 종료</span><b style={{ color: "var(--danger)" }}>[N]</b></div>
          <div className="stat"><span>현재 이용정지</span><b>[N]명</b></div>
        </div>

        <div className="row">
          <section className="card stack gap-16" style={{ flex: "2 1 520px", padding: 22 }}>
            <div className="between" style={{ gap: 8 }}>
              <h2 style={{ fontSize: 16 }}>일별 상담 · 폭언 감지 추이</h2>
              <div className="small muted" style={{ display: "flex", gap: 14 }}>
                <span style={{ display: "inline-flex", alignItems: "center", gap: 6 }}>
                  <span style={{ width: 12, height: 12, borderRadius: 3, background: "#9db0e6" }} />상담
                </span>
                <span style={{ display: "inline-flex", alignItems: "center", gap: 6 }}>
                  <span style={{ width: 12, height: 12, borderRadius: 3, background: "var(--warn)" }} />폭언
                </span>
              </div>
            </div>
            <div className="chart-placeholder">
              [막대/선 차트 영역 — 날짜별 상담 건수와 폭언 감지 건수]
            </div>
          </section>

          <section className="card stack gap-16" style={{ flex: "1 1 300px", padding: 22 }}>
            <h2 style={{ fontSize: 16 }}>상담 종료 유형</h2>
            {ENDINGS.map((label) => (
              <div key={label} className="stack gap-8">
                <div className="between" style={{ fontSize: 14, flexWrap: "nowrap" }}>
                  <span>{label}</span>
                  <span>[N]%</span>
                </div>
                <div className="bar"><div style={{ width: "0%" }} /></div>
              </div>
            ))}
            <span className="muted" style={{ fontSize: 12 }}>빈 상태: “집계할 데이터가 없습니다.”</span>
          </section>
        </div>

        <section className="card card-flush">
          <div className="between" style={{ padding: "18px 22px" }}>
            <h2 style={{ fontSize: 16 }}>최근 제재</h2>
            <Link href="/admin/sanctions" style={{ fontSize: 14 }}>전체 보기</Link>
          </div>
          <div className="table-wrap">
            <table style={{ minWidth: 640 }}>
              <thead>
                <tr>
                  <th>고객</th>
                  <th>누적 위반</th>
                  <th>제재</th>
                  <th>해제 예정일</th>
                  <th>상태</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td><Link href="/admin/sanctions/1">[고객 ID]</Link></td>
                  <td>1회</td>
                  <td>7일 정지</td>
                  <td>YYYY.MM.DD</td>
                  <td><span className="badge badge-danger">정지 중</span></td>
                </tr>
                <tr>
                  <td><Link href="/admin/sanctions/1">[고객 ID]</Link></td>
                  <td>3회</td>
                  <td>30일 정지</td>
                  <td>YYYY.MM.DD</td>
                  <td><span className="badge badge-warn">검토 필요</span></td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </main>
    </>
  );
}
