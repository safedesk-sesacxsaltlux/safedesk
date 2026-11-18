"use client";

import { useApiGet } from "../_lib/useApiGet";

const RECENT_SIZE = 3;

const HANDLED_BY = { AI: "AI 응대", AGENT: "상담원 연결" };
const STATUS = {
  CLOSED: { label: "정상 종료", className: "badge" },
  TERMINATED: { label: "자동 종료", className: "badge badge-danger" },
};

// 서버 시간 "2026-11-25T10:00:00" → "2026.11.25 10:00"
function formatDateTime(value) {
  if (!value) return "-";
  const [date, time = ""] = value.split("T");
  return `${date.replaceAll("-", ".")} ${time.slice(0, 5)}`;
}

// SCR-004 고객 메인 「최근 상담」 표 (HIS-01, 최근 3건)
export default function RecentChats() {
  const result = useApiGet("/api/customers/me/consultations", { page: 0, size: RECENT_SIZE });
  const rows = result.status === "ready" ? result.data.content ?? [] : [];

  let message = null;
  if (result.status === "loading") message = "불러오는 중…";
  else if (result.status === "error") message = "상담 내역을 불러오지 못했습니다.";
  else if (rows.length === 0) message = "아직 상담 내역이 없어요.";

  return (
    <div className="table-wrap">
      <table style={{ minWidth: 560 }}>
        <thead>
          <tr>
            <th>일시</th>
            <th>문의 요약</th>
            <th>처리</th>
            <th>상태</th>
          </tr>
        </thead>
        <tbody>
          {message ? (
            <tr>
              <td colSpan={4} className="muted" style={{ textAlign: "center" }}>
                {message}
              </td>
            </tr>
          ) : (
            rows.map((row) => {
              const status = STATUS[row.status] ?? { label: row.status, className: "badge" };
              return (
                <tr key={row.consultationId}>
                  <td>{formatDateTime(row.startedAt)}</td>
                  <td>{row.inquirySummary ?? "요약 없음"}</td>
                  <td>{HANDLED_BY[row.handledBy] ?? row.handledBy}</td>
                  <td>
                    <span className={status.className}>{status.label}</span>
                  </td>
                </tr>
              );
            })
          )}
        </tbody>
      </table>
    </div>
  );
}
