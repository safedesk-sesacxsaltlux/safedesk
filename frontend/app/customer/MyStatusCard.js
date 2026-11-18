"use client";

import { useApiGet } from "../_lib/useApiGet";

// 서버 시간 "2026-11-25T10:00:00" → "2026.11.25"
function formatDate(value) {
  return value ? value.slice(0, 10).replaceAll("-", ".") : "-";
}

// SCR-004 고객 메인 「내 이용 상태」 (HIS-02)
export default function MyStatusCard() {
  const result = useApiGet("/api/customers/me/sanction");
  const data = result.status === "ready" ? result.data : null;
  const suspended = data?.sanctionStatus === "SUSPENDED";
  const sanction = data?.currentSanction;

  let statusText = "확인 중…";
  let statusColor = "var(--muted)";
  if (result.status === "error") {
    statusText = "확인할 수 없음";
  } else if (data) {
    statusText = suspended ? "이용 정지 중" : "정상";
    statusColor = suspended ? "var(--danger)" : "var(--ok)";
  }

  return (
    <section className="card stack gap-12" style={{ flex: "1 1 280px", padding: 28, borderRadius: 16 }}>
      <h2>내 이용 상태</h2>
      <div className="kv">
        <span>제재 상태</span>
        <span style={{ fontWeight: 500, color: statusColor }}>{statusText}</span>
      </div>
      {suspended && sanction && (
        <div className="kv">
          <span>해제 예정</span>
          <span style={{ fontWeight: 500 }}>
            {formatDate(sanction.endAt)} (D-{sanction.remainingDays})
          </span>
        </div>
      )}
      <div className="kv">
        <span>누적 위반</span>
        <span style={{ fontWeight: 500 }}>{data ? `${data.totalViolationCount}회` : "-"}</span>
      </div>
      <p className="muted small" style={{ lineHeight: 1.6 }}>
        상담 중 폭언이 감지되면 1회 경고, 반복 시 상담이 자동 종료되고
        이용이 제한됩니다.
      </p>
    </section>
  );
}
