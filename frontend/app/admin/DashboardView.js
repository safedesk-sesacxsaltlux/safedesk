"use client";

// 관리자 운영 현황 대시보드 (HIS-07 GET /api/admin/dashboard?period=TODAY|7D|30D)

import Link from "next/link";
import { useState } from "react";
import { useApiGet } from "../_lib/useApiGet";
import { formatDate, formatRate } from "../_lib/format";

const PERIODS = [
  { value: "TODAY", label: "오늘" },
  { value: "7D", label: "7일" },
  { value: "30D", label: "30일" },
];

// 종료 유형 (명세 4종)
const CLOSE_TYPES = [
  { key: "aiClosed", label: "AI 응대 종료" },
  { key: "agentClosed", label: "상담원 종료" },
  { key: "queueCanceled", label: "대기 중 취소" },
  { key: "terminated", label: "강제 종료 (TERMINATED)" },
];

// 정지 상태 배지 (상태는 서버가 해제·종료 시각으로 계산)
const SANCTION_BADGE = {
  SUSPENDED: { label: "정지 중", tone: "badge-danger" },
  EXPIRED: { label: "만료", tone: "" },
  RELEASED: { label: "해제됨", tone: "badge-ok" },
};

const NO_DATA = "집계할 데이터가 없습니다.";

export default function DashboardView() {
  const [period, setPeriod] = useState("TODAY");
  const state = useApiGet("/api/admin/dashboard", { period });

  // 탭을 바꿔 새 결과를 기다리는 동안에는 직전 결과를 그대로 보여 준다 (화면이 깜빡이지 않게)
  const data = state.previous;
  const cards = data?.cards;

  return (
    <>
      <div className="between">
        <h1 style={{ fontSize: 24 }}>운영 현황 · 통계</h1>
        <div style={{ display: "flex", gap: 4, padding: 4, borderRadius: 10, background: "#fff", border: "1px solid var(--line)" }}>
          {PERIODS.map((p) => (
            <button
              key={p.value}
              type="button"
              className={`chip ${period === p.value ? "on" : ""}`}
              style={{ borderRadius: 8, minHeight: 40, fontSize: 14, border: period === p.value ? undefined : 0 }}
              onClick={() => setPeriod(p.value)}
            >
              {p.label}
            </button>
          ))}
        </div>
      </div>

      {data && (
        <span className="small muted">
          {formatDate(data.period.from)} ~ {formatDate(data.period.to)}
          {state.status === "loading" && " · 불러오는 중"}
        </span>
      )}
      {state.status === "error" && <span className="msg-err">{state.message}</span>}

      <div className="stats">
        <div className="stat"><span>전체 상담</span><b>{cards ? cards.totalConsultationCount : "—"}</b></div>
        <div className="stat"><span>AI 단독 해결</span><b>{cards ? formatRate(cards.aiOnlyResolvedRate) : "—"}</b></div>
        <div className="stat"><span>폭언 감지</span><b style={{ color: "var(--warn)" }}>{cards ? cards.abuseDetectedCount : "—"}</b></div>
        <div className="stat"><span>자동 종료</span><b style={{ color: "var(--danger)" }}>{cards ? cards.autoTerminatedCount : "—"}</b></div>
        <div className="stat"><span>신규 제재</span><b style={{ color: "var(--danger)" }}>{cards ? `${cards.newSanctionCount}건` : "—"}</b></div>
        <div className="stat"><span>현재 이용정지</span><b>{cards ? `${cards.currentSuspendedCustomerCount}명` : "—"}</b></div>
      </div>

      <div className="row">
        <section className="card stack gap-16" style={{ flex: "2 1 520px", padding: 22 }}>
          <div className="between" style={{ gap: 8 }}>
            <h2 style={{ fontSize: 16 }}>일별 상담 · 폭언 감지 추이</h2>
            <div className="small muted" style={{ display: "flex", gap: 14 }}>
              <Legend color="#9db0e6" label="상담" />
              <Legend color="var(--warn)" label="폭언" />
            </div>
          </div>
          {data ? <TrendChart trend={data.dailyTrend} /> : <div className="chart-placeholder">불러오는 중</div>}
        </section>

        <section className="card stack gap-16" style={{ flex: "1 1 300px", padding: 22 }}>
          <h2 style={{ fontSize: 16 }}>상담 종료 유형</h2>
          {data && data.closeTypes.totalClosedCount === 0 && <span className="muted small">{NO_DATA}</span>}
          {data && data.closeTypes.totalClosedCount > 0 && CLOSE_TYPES.map((t) => {
            const item = data.closeTypes[t.key];
            return (
              <div key={t.key} className="stack gap-8">
                <div className="between" style={{ fontSize: 14, flexWrap: "nowrap" }}>
                  <span>{t.label}</span>
                  <span>{item.count}건 · {formatRate(item.rate)}</span>
                </div>
                <div className="bar"><div style={{ width: `${item.rate ?? 0}%` }} /></div>
              </div>
            );
          })}
          {data && data.closeTypes.totalClosedCount > 0 && (
            <span className="muted" style={{ fontSize: 12 }}>기간 내 종료된 상담 {data.closeTypes.totalClosedCount}건 기준</span>
          )}
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
              {data?.recentSanctions.map((s) => {
                const badge = SANCTION_BADGE[s.status] ?? { label: s.status, tone: "" };
                return (
                  <tr key={s.sanctionId}>
                    <td><Link href={`/admin/sanctions/${s.sanctionId}`}>{s.customerLoginId}</Link></td>
                    <td>{s.violationCount}회</td>
                    <td>{s.suspendDays}일 정지</td>
                    <td>{formatDate(s.endAt)}</td>
                    <td><span className={`badge ${badge.tone}`}>{badge.label}</span></td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
        {data && data.recentSanctions.length === 0 && <div className="card-foot">최근 제재가 없습니다.</div>}
      </section>
    </>
  );
}

function Legend({ color, label }) {
  return (
    <span style={{ display: "inline-flex", alignItems: "center", gap: 6 }}>
      <span style={{ width: 12, height: 12, borderRadius: 3, background: color }} />{label}
    </span>
  );
}

// 날짜별 막대 두 개(상담·폭언). 라이브러리 없이 CSS 로 그린다. 높이는 기간 중 가장 큰 값 기준
function TrendChart({ trend }) {
  const max = Math.max(1, ...trend.map((d) => Math.max(d.consultationCount, d.abuseDetectedCount)));
  const allZero = trend.every((d) => d.consultationCount === 0 && d.abuseDetectedCount === 0);
  // 30일이면 날짜 글자가 겹치므로 5일마다 + 마지막 날만 표시
  const labelEvery = trend.length > 10 ? 5 : 1;

  return (
    <div className="stack gap-8">
      <div style={{ display: "flex", alignItems: "flex-end", gap: trend.length > 10 ? 3 : 10, height: 180,
        borderBottom: "1px solid var(--line)", padding: "0 4px" }}>
        {trend.map((d) => (
          <div key={d.date} title={`${formatDate(d.date)} · 상담 ${d.consultationCount} · 폭언 ${d.abuseDetectedCount}`}
            style={{ flex: 1, display: "flex", alignItems: "flex-end", justifyContent: "center", gap: 2, height: "100%" }}>
            <div style={{ width: "45%", maxWidth: 18, height: `${(d.consultationCount / max) * 100}%`,
              background: "#9db0e6", borderRadius: "3px 3px 0 0", minHeight: d.consultationCount ? 2 : 0 }} />
            <div style={{ width: "45%", maxWidth: 18, height: `${(d.abuseDetectedCount / max) * 100}%`,
              background: "var(--warn)", borderRadius: "3px 3px 0 0", minHeight: d.abuseDetectedCount ? 2 : 0 }} />
          </div>
        ))}
      </div>
      <div style={{ display: "flex", gap: trend.length > 10 ? 3 : 10, padding: "0 4px" }}>
        {trend.map((d, i) => (
          <span key={d.date} className="muted" style={{ flex: 1, textAlign: "center", fontSize: 11, whiteSpace: "nowrap" }}>
            {i % labelEvery === 0 || i === trend.length - 1 ? d.date.slice(5).replace("-", ".") : ""}
          </span>
        ))}
      </div>
      {allZero && <span className="muted small">{NO_DATA}</span>}
    </div>
  );
}
