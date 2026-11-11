import Link from "next/link";
import { AdminHeader } from "../../_components/Headers";

export const metadata = { title: "상담 이력 · SafeDesk 관리자" };

const ROWS = [
  { agent: "[상담원명]", kind: "상담원 연결", count: 2, status: "TERMINATED", tone: "badge-danger" },
  { agent: "[상담원명]", kind: "상담원 연결", count: 0, status: "CLOSED", tone: "badge-ok" },
  { agent: "—", kind: "AI 응대", count: 0, status: "BOT 종료", tone: "" },
  { agent: "[상담원명]", kind: "상담원 연결", count: 1, status: "CLOSED", tone: "badge-ok" },
];

export default function AdminHistoryPage() {
  return (
    <>
      <AdminHeader active="history" />
      <main className="container-wide main">
        <div className="stack gap-4">
          <h1 style={{ fontSize: 24 }}>상담 이력</h1>
          <span className="small muted">
            관리자: 전체 상담 · 상담원: 본인이 응대한 상담만 · 고객: 본인 상담만(내 상담 이력)
          </span>
        </div>

        <div className="filter">
          <div className="field" style={{ flex: "1 1 220px" }}>
            <label htmlFor="hs-from">기간</label>
            <div className="date-pair">
              <input id="hs-from" className="input sm" type="date" />
              <span>~</span>
              <input className="input sm" type="date" aria-label="종료일" />
            </div>
          </div>
          <div className="field">
            <label htmlFor="hs-st">종료 상태</label>
            <select id="hs-st" className="input sm">
              <option>전체</option>
              <option>BOT 종료</option>
              <option>CLOSED</option>
              <option>TERMINATED</option>
            </select>
          </div>
          <div className="field">
            <label htmlFor="hs-ag">상담원</label>
            <select id="hs-ag" className="input sm">
              <option>전체</option>
            </select>
          </div>
          <div className="field" style={{ flex: "2 1 200px" }}>
            <label htmlFor="hs-q">검색</label>
            <input id="hs-q" className="input sm" type="text" placeholder="상담번호 · 고객 ID" />
          </div>
          <button type="button" className="btn btn-dark" style={{ minHeight: 44, fontSize: 14 }}>조회</button>
        </div>

        <section className="card card-flush">
          <div className="table-wrap">
            <table style={{ minWidth: 860 }}>
              <thead>
                <tr>
                  <th>상담번호</th>
                  <th>시작 일시</th>
                  <th>고객</th>
                  <th>상담원</th>
                  <th>처리</th>
                  <th>폭언 감지</th>
                  <th>상태</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {ROWS.map((r, i) => (
                  <tr key={i}>
                    <td>[C-000000]</td>
                    <td>YYYY.MM.DD HH:MM</td>
                    <td>[고객 ID]</td>
                    <td>{r.agent}</td>
                    <td>{r.kind}</td>
                    <td>{r.count}</td>
                    <td><span className={`badge ${r.tone}`}>{r.status}</span></td>
                    <td><Link href="/admin/history/1">상세</Link></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="card-foot">빈 상태: “조건에 맞는 상담 이력이 없습니다.”</div>
        </section>
      </main>
    </>
  );
}
