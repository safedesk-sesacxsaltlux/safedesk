"use client";

// 관리자 제재 목록 (API-ADM-04 GET /api/admin/sanctions)

import { useState } from "react";
import { useApiGet } from "../../_lib/useApiGet";
import { formatDate } from "../../_lib/format";
import Pager from "../../_components/Pager";
import RowLink from "../../_components/RowLink";

const EMPTY_FILTER = { keyword: "", status: "ALL", durationDays: "ALL" };

// 상태는 서버가 해제·종료 시각으로 계산한다 (「검토 필요」 상태는 쓰지 않음, policy.md)
const STATUS_BADGE = {
  SUSPENDED: { label: "정지 중", tone: "badge-danger" },
  RELEASED: { label: "해제됨", tone: "badge-ok" },
  EXPIRED: { label: "기간 만료", tone: "" },
};

export default function SanctionsView() {
  const [form, setForm] = useState(EMPTY_FILTER);       // 입력 중인 값
  const [filter, setFilter] = useState(EMPTY_FILTER);   // 「조회」를 누른 값 (이 값으로 요청)
  const [page, setPage] = useState(0);
  const state = useApiGet("/api/admin/sanctions", { ...filter, page, size: 20 });

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
        <div className="field" style={{ flex: "2 1 220px" }}>
          <label htmlFor="sc-q">고객 검색</label>
          <input id="sc-q" name="keyword" className="input sm" type="text" placeholder="고객 ID 또는 이름"
            value={form.keyword} onChange={onChange} />
        </div>
        <div className="field">
          <label htmlFor="sc-st">상태</label>
          <select id="sc-st" name="status" className="input sm" value={form.status} onChange={onChange}>
            <option value="ALL">전체</option>
            <option value="SUSPENDED">정지 중</option>
            <option value="RELEASED">해제됨</option>
            <option value="EXPIRED">기간 만료</option>
          </select>
        </div>
        <div className="field">
          <label htmlFor="sc-tp">제재 유형</label>
          <select id="sc-tp" name="durationDays" className="input sm" value={form.durationDays} onChange={onChange}>
            <option value="ALL">전체</option>
            <option value="7">7일 정지</option>
            <option value="30">30일 정지</option>
            <option value="90">90일 정지</option>
          </select>
        </div>
        <button type="submit" className="btn btn-dark" style={{ minHeight: 44, fontSize: 14 }}>조회</button>
      </form>

      <section className="card card-flush">
        {state.status === "loading" && <div className="card-foot">제재 목록을 불러오는 중입니다.</div>}
        {state.status === "error" && <div className="card-foot msg-err">{state.message}</div>}
        {state.status === "ready" && (
          <>
            <div style={{ padding: "14px 18px", fontSize: 14, color: "var(--muted)" }}>
              총 {state.data.totalElements}건
            </div>
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
                  {state.data.content.map((s) => {
                    const badge = STATUS_BADGE[s.status] ?? { label: s.status, tone: "" };
                    return (
                      <tr key={s.sanctionId}>
                        <td>{s.customer.loginId}</td>
                        <td>{s.violationCount}회</td>
                        <td>{formatDate(s.lastViolatedAt)}</td>
                        <td>{s.durationDays}일 정지</td>
                        <td>{formatDate(s.startsAt)} ~ {formatDate(s.endsAt)}</td>
                        <td><span className={`badge ${badge.tone}`}>{badge.label}</span></td>
                        <td style={{ textAlign: "center" }}><RowLink href={`/admin/sanctions/${s.sanctionId}`} label="제재 상세" /></td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
            {state.data.content.length === 0 && <div className="card-foot">조건에 맞는 제재가 없습니다.</div>}
            <Pager page={state.data.page} totalPages={state.data.totalPages} hasNext={state.data.hasNext}
              onChange={setPage} />
          </>
        )}
      </section>
    </>
  );
}
