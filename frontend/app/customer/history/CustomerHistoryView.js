"use client";

// 고객 내 상담 이력 (HIS-01 GET /api/customers/me/consultations)

import Link from "next/link";
import { useState } from "react";
import { useApiGet } from "../../_lib/useApiGet";
import { RESULT_LABEL, formatDateTime } from "../../_lib/format";
import Pager from "../../_components/Pager";

const TYPES = [
  { value: "ALL", label: "전체" },
  { value: "AI", label: "AI 응대" },
  { value: "AGENT", label: "상담원 연결" },
];

function statusBadge(item) {
  if (item.status === "TERMINATED") return { label: "강제 종료", tone: "badge-danger" };
  if (item.handledBy === "AI") return { label: "AI 응대 완료", tone: "" };
  return { label: "상담 완료", tone: "badge-ok" };
}

export default function CustomerHistoryView() {
  const [type, setType] = useState("ALL");
  const [page, setPage] = useState(0);
  const state = useApiGet("/api/customers/me/consultations", { type, page, size: 20 });

  function changeType(value) {
    setType(value);
    setPage(0);
  }

  return (
    <>
      <div className="chip-row">
        {TYPES.map((t) => (
          <button key={t.value} type="button" className={`chip ${type === t.value ? "on" : ""}`}
            onClick={() => changeType(t.value)}>
            {t.label}
          </button>
        ))}
      </div>

      {state.status === "loading" && <span className="small muted">상담 이력을 불러오는 중입니다.</span>}
      {state.status === "error" && <span className="msg-err">{state.message}</span>}

      {state.status === "ready" && state.data.content.length === 0 && (
        <div className="dashed" style={{ textAlign: "center" }}>
          아직 상담 이력이 없어요. <Link href="/customer">상담 시작하기</Link>
        </div>
      )}

      {state.status === "ready" && state.data.content.map((c) => {
        const badge = statusBadge(c);
        return (
          <details key={c.consultationId} className="card accordion">
            <summary>
              <div className="stack gap-4">
                <span className="small muted">
                  {formatDateTime(c.startedAt)} · {c.handledBy === "AI" ? "AI 응대" : "상담원 연결"}
                </span>
                <span className="bold">{c.inquirySummary ?? "문의 내용 없음"}</span>
                {c.violationRecorded && (
                  <span className="small" style={{ color: "var(--danger)" }}>
                    상담 중 부적절한 표현으로 종료됨 · 위반 1회 누적
                  </span>
                )}
              </div>
              <span className={`badge ${badge.tone}`}>{badge.label}</span>
            </summary>
            <div className="acc-body">
              <div className="transcript">
                <span className="bold">상담 요약</span>
                {c.summary ? (
                  <>
                    <span className="muted">문의 유형: {c.summary.inquiryCategory ?? "—"}</span>
                    <span className="muted">처리 내용: {c.summary.processSummary ?? "—"}</span>
                    <span className="muted">처리 결과: {RESULT_LABEL[c.summary.result] ?? "—"}</span>
                  </>
                ) : (
                  <span className="muted">요약 없음</span>
                )}
              </div>
            </div>
          </details>
        );
      })}

      {state.status === "ready" && (
        <Pager page={state.data.page} totalPages={state.data.totalPages} hasNext={state.data.hasNext}
          onChange={setPage} />
      )}
    </>
  );
}
