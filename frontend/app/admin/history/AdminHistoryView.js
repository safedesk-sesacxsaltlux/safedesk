"use client";

// 관리자 전체 상담 이력 (HIS-05 GET /api/admin/consultations)

import { useState } from "react";
import { useApiGet } from "../../_lib/useApiGet";
import { formatDateTime, statusBadge } from "../../_lib/format";
import Pager from "../../_components/Pager";
import RowLink from "../../_components/RowLink";

const EMPTY_FILTER = { from: "", to: "", status: "ALL", handledBy: "ALL", keyword: "" };
const ERROR_MESSAGE = { INVALID_PERIOD: "조회 기간을 다시 선택해주세요." };

export default function AdminHistoryView() {
  const [form, setForm] = useState(EMPTY_FILTER);       // 입력 중인 값
  const [filter, setFilter] = useState(EMPTY_FILTER);   // "조회"를 누른 값 (이 값으로 요청)
  const [page, setPage] = useState(0);
  const result = useApiGet("/api/admin/consultations", { ...filter, page, size: 20 });
  const state = result.status === "error"
    ? { ...result, message: ERROR_MESSAGE[result.errorCode] ?? result.message }
    : result;

  function onChange(e) {
    setForm({ ...form, [e.target.name]: e.target.value });
  }

  function onSearch(e) {
    e.preventDefault();
    setPage(0);
    setFilter(form);
  }

  return (
    <>
      <form className="filter" onSubmit={onSearch}>
        <div className="field" style={{ flex: "1 1 220px" }}>
          <label htmlFor="hs-from">기간 (상담 시작일)</label>
          <div className="date-pair">
            <input id="hs-from" name="from" className="input sm" type="date" value={form.from} onChange={onChange} />
            <span>~</span>
            <input name="to" className="input sm" type="date" aria-label="종료일" value={form.to} onChange={onChange} />
          </div>
        </div>
        <div className="field">
          <label htmlFor="hs-st">종료 상태</label>
          <select id="hs-st" name="status" className="input sm" value={form.status} onChange={onChange}>
            <option value="ALL">전체</option>
            <option value="CLOSED">종료 (CLOSED)</option>
            <option value="TERMINATED">강제 종료 (TERMINATED)</option>
          </select>
        </div>
        <div className="field">
          <label htmlFor="hs-hb">처리</label>
          <select id="hs-hb" name="handledBy" className="input sm" value={form.handledBy} onChange={onChange}>
            <option value="ALL">전체</option>
            <option value="AI">AI 응대</option>
            <option value="AGENT">상담원 연결</option>
          </select>
        </div>
        <div className="field" style={{ flex: "2 1 200px" }}>
          <label htmlFor="hs-q">검색</label>
          <input id="hs-q" name="keyword" className="input sm" type="text" placeholder="상담번호 · 고객 ID"
            value={form.keyword} onChange={onChange} />
        </div>
        <button type="submit" className="btn btn-dark" style={{ minHeight: 44, fontSize: 14 }}>조회</button>
      </form>

      <section className="card card-flush">
        {state.status === "loading" && <div className="card-foot">상담 이력을 불러오는 중입니다.</div>}
        {state.status === "error" && <div className="card-foot msg-err">{state.message}</div>}
        {state.status === "ready" && (
          <>
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
                  {state.data.content.map((r) => {
                    const badge = statusBadge(r.status);
                    return (
                      <tr key={r.consultationId}>
                        <td>{r.consultationNo}</td>
                        <td>{formatDateTime(r.startedAt)}</td>
                        <td>{r.customerLoginId}</td>
                        <td>{r.agentName ?? "—"}</td>
                        <td>{r.handledBy === "AI" ? "AI 응대" : "상담원 연결"}</td>
                        <td style={r.abuseCount ? { color: "var(--warn)", fontWeight: 700 } : undefined}>{r.abuseCount}</td>
                        <td><span className={`badge ${badge.tone}`}>{badge.label}</span></td>
                        <td style={{ textAlign: "center" }}><RowLink href={`/admin/history/${r.consultationId}`} label={`${r.consultationNo} 상세`} /></td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
            {state.data.content.length === 0 && <div className="card-foot">조건에 맞는 상담 이력이 없습니다.</div>}
            <div className="card-foot">
              <span>총 {state.data.totalElements}건</span>
            </div>
            <Pager page={state.data.page} totalPages={state.data.totalPages} hasNext={state.data.hasNext} onChange={setPage} />
          </>
        )}
      </section>
    </>
  );
}
