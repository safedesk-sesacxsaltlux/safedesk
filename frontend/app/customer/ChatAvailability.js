"use client";

import { useApiGet } from "../_lib/useApiGet";

// SCR-004 「새 상담 시작하기」 카드의 상담 가능 여부 (HIS-02 canStartConsultation)
// 정지 중이어도 버튼은 그대로 둔다. 누르면 CUS-01 이 이용정지 안내 화면으로 보낸다.
export default function ChatAvailability() {
  const result = useApiGet("/api/customers/me/sanction");
  const blocked = result.status === "ready" && !result.data.canStartConsultation;
  const color = blocked ? "var(--danger)" : "var(--ok)";

  return (
    <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
      <span style={{ width: 10, height: 10, borderRadius: "50%", background: color }} />
      <span style={{ fontSize: 14, color, fontWeight: 500 }}>{blocked ? "이용 정지 중" : "상담 가능"}</span>
    </div>
  );
}
