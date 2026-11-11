import Link from "next/link";
import { AdminHeader } from "../../_components/Headers";

export const metadata = { title: "제재 관리 · SafeDesk 관리자" };

const ROWS = [
  { count: "3회", type: "30일 정지", status: "검토 필요", tone: "badge-warn" },
  { count: "1회", type: "7일 정지", status: "정지 중", tone: "badge-danger" },
  { count: "2회", type: "30일 정지", status: "정지 중", tone: "badge-danger" },
  { count: "1회", type: "7일 정지", status: "해제됨", tone: "badge-ok" },
];

export default function SanctionsPage() {
  return (
    <>
      <AdminHeader active="sanctions" />
      <main className="container-wide main">
        <h1 style={{ fontSize: 24 }}>제재 관리</h1>

        <div className="filter">
          <div className="field" style={{ flex: "2 1 220px" }}>
            <label htmlFor="sc-q">고객 검색</label>
            <input id="sc-q" className="input sm" type="text" placeholder="고객 ID 또는 이름" />
          </div>
          <div className="field">
            <label htmlFor="sc-st">상태</label>
            <select id="sc-st" className="input sm">
              <option>전체</option>
              <option>정지 중</option>
              <option>검토 필요</option>
              <option>해제됨</option>
              <option>기간 만료</option>
            </select>
          </div>
          <div className="field">
            <label htmlFor="sc-tp">제재 유형</label>
            <select id="sc-tp" className="input sm">
              <option>전체</option>
              <option>7일 정지</option>
              <option>30일 정지</option>
            </select>
          </div>
          <button type="button" className="btn btn-dark" style={{ minHeight: 44, fontSize: 14 }}>조회</button>
        </div>

        <section className="card card-flush">
          <div style={{ padding: "14px 18px", fontSize: 14, color: "var(--muted)" }}>총 [N]건</div>
          <div className="table-wrap">
            <table style={{ minWidth: 760 }}>
              <thead>
                <tr>
                  <th>고객 ID</th>
                  <th>누적 위반</th>
                  <th>최근 위반일</th>
                  <th>제재 유형</th>
                  <th>제재 기간</th>
                  <th>상태</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {ROWS.map((r, i) => (
                  <tr key={i}>
                    <td>[고객 ID]</td>
                    <td>{r.count}</td>
                    <td>YYYY.MM.DD</td>
                    <td>{r.type}</td>
                    <td>MM.DD ~ MM.DD</td>
                    <td><span className={`badge ${r.tone}`}>{r.status}</span></td>
                    <td><Link href="/admin/sanctions/1">상세</Link></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="pager">
            <button type="button" aria-label="이전 페이지">‹</button>
            <button type="button" className="on">1</button>
            <button type="button">2</button>
            <button type="button" aria-label="다음 페이지">›</button>
          </div>
        </section>
      </main>
    </>
  );
}
