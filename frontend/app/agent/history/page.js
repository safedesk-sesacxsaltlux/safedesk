import { AgentHeader } from "../../_components/Headers";

export const metadata = { title: "상담 이력 · SafeDesk 상담원" };

const ROWS = [
  { flagged: true, count: 2, status: "TERMINATED", tone: "badge-danger" },
  { flagged: false, count: 0, status: "CLOSED", tone: "badge-ok" },
  { flagged: false, count: 1, status: "CLOSED", tone: "badge-ok" },
  { flagged: false, count: 0, status: "CLOSED", tone: "badge-ok" },
];

export default function AgentHistoryPage() {
  return (
    <>
      <AgentHeader active="history" />
      <main className="container-wide main" style={{ gap: 18 }}>
        <div className="stack gap-4">
          <h1 style={{ fontSize: 24 }}>내 상담 이력</h1>
          <span className="small muted">내가 응대한 고객만 표시됩니다 · 고객별 최근 상담 기준</span>
        </div>

        <div className="stats" style={{ gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))" }}>
          <div className="stat"><span>응대한 고객</span><b>[N]명</b></div>
          <div className="stat"><span>총 상담 건수</span><b>[N]건</b></div>
          <div className="stat"><span>평균 상담 시간</span><b>[MM:SS]</b></div>
          <div className="stat"><span>폭언 자동 종료</span><b style={{ color: "var(--warn)" }}>[N]건</b></div>
        </div>

        <div className="filter">
          <div className="field" style={{ flex: "1 1 200px" }}>
            <label htmlFor="ah-from">기간</label>
            <div className="date-pair">
              <input id="ah-from" className="input sm" type="date" />
              <span>~</span>
              <input className="input sm" type="date" aria-label="종료일" />
            </div>
          </div>
          <div className="field">
            <label htmlFor="ah-st">최근 종료 상태</label>
            <select id="ah-st" className="input sm">
              <option>전체</option>
              <option>CLOSED</option>
              <option>TERMINATED</option>
            </select>
          </div>
          <div className="field" style={{ flex: "2 1 220px" }}>
            <label htmlFor="ah-q">고객 검색</label>
            <input id="ah-q" className="input sm" type="text" placeholder="고객 ID · 상담번호" />
          </div>
          <button type="button" className="btn btn-dark" style={{ minHeight: 44, fontSize: 14 }}>조회</button>
        </div>

        <section className="card card-flush">
          <div className="table-wrap">
            <table style={{ minWidth: 880 }}>
              <thead>
                <tr>
                  <th>고객</th>
                  <th>최근 상담 일시</th>
                  <th>최근 상담번호</th>
                  <th>내 응대 횟수</th>
                  <th>최근 문의 요약</th>
                  <th>폭언 감지</th>
                  <th>최근 상태</th>
                </tr>
              </thead>
              <tbody>
                {ROWS.map((r, i) => (
                  <tr key={i}>
                    <td>
                      [고객 ID]
                      {r.flagged && (
                        <span className="badge badge-warn" style={{ marginLeft: 6 }}>위반 이력</span>
                      )}
                    </td>
                    <td>YYYY.MM.DD HH:MM</td>
                    <td>[C-000000]</td>
                    <td>[N]회</td>
                    <td>[문의 요약]</td>
                    <td style={r.count ? { color: "var(--warn)", fontWeight: 700 } : undefined}>{r.count}</td>
                    <td><span className={`badge ${r.tone}`}>{r.status}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="card-foot">
            <span>빈 상태: “아직 응대한 고객이 없습니다.”</span>
            <span>폭언 원문은 마스킹되어 표시되며, 원문 열람은 관리자만 가능합니다.</span>
          </div>
        </section>
      </main>
    </>
  );
}
