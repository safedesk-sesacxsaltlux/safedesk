// 이력·통계 화면에서 쓰는 표시용 변환 함수

// "2026-11-12T14:20:05" → "2026.11.12 14:20"
export function formatDateTime(value) {
  if (!value) return "—";
  return value.slice(0, 16).replace("T", " ").replaceAll("-", ".");
}

// "2026-11-12T14:20:05" → "14:20"
export function formatTime(value) {
  if (!value) return "";
  return value.slice(11, 16);
}

// "2026-11-12T14:20:05" 또는 "2026-11-12" → "2026.11.12"
export function formatDate(value) {
  if (!value) return "—";
  return value.slice(0, 10).replaceAll("-", ".");
}

// 338 → "5분 38초"
export function formatDuration(seconds) {
  if (!seconds) return "0초";
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return m ? `${m}분 ${s}초` : `${s}초`;
}

// 상담 상태 배지
export const STATUS_BADGE = {
  CLOSED: { label: "종료", tone: "badge-ok" },
  TERMINATED: { label: "강제 종료", tone: "badge-danger" },
};

export function statusBadge(status) {
  return STATUS_BADGE[status] ?? { label: status, tone: "" };
}

// 처리 결과 (상담 요약)
export const RESULT_LABEL = {
  RESOLVED: "해결",
  UNRESOLVED: "미해결",
  TERMINATED: "강제 종료",
};

// 종료 사유 문구 (요약 템플릿과 같은 표현)
export function closeReasonLabel(closeReason, closedBy) {
  switch (closeReason) {
    case "NORMAL":
      return closedBy === "AGENT" ? "상담원 종료" : closedBy === "CUSTOMER" ? "고객 종료" : "상담 종료";
    case "QUEUE_CANCEL":
      return "대기 중 취소";
    case "ABUSE_AUTO":
      return "반복 폭언으로 자동 종료";
    case "ABUSE_MANUAL":
      return "폭언으로 상담원 종료";
    default:
      return "—";
  }
}

// 비율 (분모가 0 이면 null → "—")
export function formatRate(rate) {
  return rate === null || rate === undefined ? "—" : `${rate}%`;
}
