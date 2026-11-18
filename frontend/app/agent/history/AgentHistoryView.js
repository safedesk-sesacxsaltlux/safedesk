"use client";

// 상담원 내 상담 이력
//  - 상단 카드 + 고객별 목록: HIS-03 GET /api/agents/me/customers
//  - 고객 행을 누르면 그 고객과 나의 상담 목록: HIS-04 GET /api/agents/me/consultations?customerId=
//  - 상담 행의 "상세": HIS-06 (/agent/history/{id})

import Link from "next/link";
import { Fragment, useState } from "react";
import { useApiGet } from "../../_lib/useApiGet";
import { formatDateTime, formatDuration, statusBadge } from "../../_lib/format";
import Pager from "../../_components/Pager";
import Icon from "../../_components/Icon";

const EMPTY_FILTER = { from: "", to: "", status: "ALL", keyword: "" };
const ERROR_MESSAGE = { INVALID_PERIOD: "조회 기간을 다시 선택해주세요. (최대 366일)" };

export default function AgentHistoryView() {
  const [form, setForm] = useState(EMPTY_FILTER);
  const [filter, setFilter] = useState(EMPTY_FILTER);
  const [page, setPage] = useState(0);
  const [openCustomerId, setOpenCustomerId] = useState(null);
  const result = useApiGet("/api/agents/me/customers", { ...filter, page, size: 20 });
  const state = result.status === "error"
    ? { ...result, message: ERROR_MESSAGE[result.errorCode] ?? result.message }
    : result;

  function onChange(e) {
    setForm({ ...form, [e.target.name]: e.target.value });
  }

  function onSearch(e) {
    e.preventDefault();
    setPage(0);
    setOpenCustomerId(null);
    setFilter(form);
  }

  const stats = state.status === "ready" ? state.data.stats : null;
  const customers = state.status === "ready" ? state.data.customers : null;

  return (
    <>
      <div className="stats" style={{ gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))" }}>
        <div className="stat"><span>응대한 고객</span><b>{stats ? `${stats.servedCustomerCount}명` : "—"}</b></div>
        <div className="stat"><span>총 상담 건수</span><b>{stats ? `${stats.totalConsultationCount}건` : "—"}</b></div>
        <div className="stat"><span>평균 상담 시간</span><b>{stats ? formatDuration(stats.avgDurationSeconds) : "—"}</b></div>
        <div className="stat"><span>강제 종료</span><b style={{ color: "var(--warn)" }}>{stats ? `${stats.abuseTerminatedCount}건` : "—"}</b></div>
      </div>

      <form className="filter" onSubmit={onSearch}>
        <div className="field" style={{ flex: "1 1 200px" }}>
          <label htmlFor="ah-from">기간 (상담 시작일)</label>
          <div className="date-pair">
            <input id="ah-from" name="from" className="input sm" type="date" value={form.from} onChange={onChange} />
            <span>~</span>
            <input name="to" className="input sm" type="date" aria-label="종료일" value={form.to} onChange={onChange} />
          </div>
        </div>
        <div className="field">
          <label htmlFor="ah-st">최근 종료 상태</label>
          <select id="ah-st" name="status" className="input sm" value={form.status} onChange={onChange}>
            <option value="ALL">전체</option>
            <option value="CLOSED">종료 (CLOSED)</option>
            <option value="TERMINATED">강제 종료 (TERMINATED)</option>
          </select>
        </div>
        <div className="field" style={{ flex: "2 1 220px" }}>
          <label htmlFor="ah-q">고객 검색</label>
          <input id="ah-q" name="keyword" className="input sm" type="text" placeholder="고객 ID · 상담번호"
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
                  {customers.content.map((r) => {
                    const latest = r.latestConsultation;
                    const badge = statusBadge(latest.status);
                    const open = openCustomerId === r.customerId;
                    return (
                      <Fragment key={r.customerId}>
                        <tr
                          onClick={() => setOpenCustomerId(open ? null : r.customerId)}
                          style={{ cursor: "pointer", background: open ? "var(--line-soft)" : undefined }}
                          title="눌러서 이 고객과의 상담 목록 보기"
                        >
                          <td>
                            {open ? "▾ " : "▸ "}{r.customerLoginId}
                            {r.hasViolationHistory && (
                              <span className="badge badge-warn" style={{ marginLeft: 6 }}>위반 이력</span>
                            )}
                          </td>
                          <td>{formatDateTime(latest.startedAt)}</td>
                          <td>{latest.consultationNo}</td>
                          <td>{r.myConsultationCount}회</td>
                          <td>{latest.inquirySummary ?? "—"}</td>
                          <td style={latest.abuseCount ? { color: "var(--warn)", fontWeight: 700 } : undefined}>{latest.abuseCount}</td>
                          <td><span className={`badge ${badge.tone}`}>{badge.label}</span></td>
                        </tr>
                        {open && (
                          <tr>
                            <td colSpan={7} style={{ background: "var(--line-soft)", padding: "8px 16px 16px" }}>
                              <CustomerConsultations customerId={r.customerId} filter={filter} />
                            </td>
                          </tr>
                        )}
                      </Fragment>
                    );
                  })}
                </tbody>
              </table>
            </div>
            {customers.content.length === 0 && <div className="card-foot">아직 응대한 고객이 없습니다.</div>}
            <div className="card-foot">
              <span>고객 행을 누르면 그 고객과의 상담 목록이 펼쳐집니다.</span>
              <span>폭언 원문은 마스킹되어 표시되며, 원문 열람은 관리자만 가능합니다.</span>
            </div>
            <Pager page={customers.page} totalPages={customers.totalPages} hasNext={customers.hasNext} onChange={setPage} />
          </>
        )}
      </section>
    </>
  );
}

// 고객 한 명과 나의 상담 목록 (HIS-04). 위 목록과 같은 기간 필터를 쓴다
function CustomerConsultations({ customerId, filter }) {
  const [page, setPage] = useState(0);
  const state = useApiGet("/api/agents/me/consultations",
    { customerId, from: filter.from, to: filter.to, page, size: 10 });

  if (state.status === "loading") return <span className="small muted">상담 목록을 불러오는 중입니다.</span>;
  if (state.status === "error") return <span className="msg-err">{state.message}</span>;

  const { content } = state.data;
  if (content.length === 0) return <span className="small muted">상담이 없습니다.</span>;

  return (
    <div className="stack gap-8">
      <table>
        <thead>
          <tr>
            <th>상담번호</th>
            <th>시작</th>
            <th>종료</th>
            <th>문의 요약</th>
            <th>폭언 감지</th>
            <th>상태</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {content.map((c) => {
            const badge = statusBadge(c.status);
            return (
              <tr key={c.consultationId}>
                <td>{c.consultationNo}</td>
                <td>{formatDateTime(c.startedAt)}</td>
                <td>{formatDateTime(c.closedAt)}</td>
                <td>{c.inquirySummary ?? "—"}</td>
                <td>{c.abuseCount}</td>
                <td><span className={`badge ${badge.tone}`}>{badge.label}</span></td>
                <td>
                  <Link href={`/agent/history/${c.consultationId}`} onClick={(e) => e.stopPropagation()}
                    aria-label="상세 보기" title="상세 보기" style={{ display: "inline-flex" }}>
                    <Icon name="next" size={18} />
                  </Link>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
      <Pager page={state.data.page} totalPages={state.data.totalPages} hasNext={state.data.hasNext} onChange={setPage} />
    </div>
  );
}
