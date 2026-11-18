"use client";

import { useSearchParams } from "next/navigation";
import Icon from "../../_components/Icon";

const DAY_MS = 24 * 60 * 60 * 1000;

// 서버 시간 "2026-11-25T10:00:00" (KST, 타임존 표기 없음) → "2026.11.25 10:00"
function formatDateTime(value) {
  const [date, time = ""] = value.split("T");
  return `${date.replaceAll("-", ".")} ${time.slice(0, 5)}`;
}

// 해제까지 남은 날 (해제일 당일이면 D-day)
function daysLeft(value) {
  const release = new Date(value);
  const today = new Date();
  const days = Math.ceil(
    (new Date(release.getFullYear(), release.getMonth(), release.getDate()) -
      new Date(today.getFullYear(), today.getMonth(), today.getDate())) / DAY_MS,
  );
  return days > 0 ? `D-${days}` : "D-day";
}

// CUS-01 이 403 CUSTOMER_SANCTIONED 와 함께 준 해제 일시·사유를 보여준다.
// 상담 시작 버튼이 주소에 담아 넘긴다. 주소 없이 들어오면 값 대신 안내만 보인다.
export default function SuspensionNotice() {
  const searchParams = useSearchParams();
  const releaseAt = searchParams.get("releaseAt");
  const reason = searchParams.get("reason");
  const valid = releaseAt && !Number.isNaN(new Date(releaseAt).getTime());

  return (
    <section className="card stack gap-20" style={{ padding: 36, borderRadius: 16, alignItems: "center", textAlign: "center" }}>
      <div
        style={{
          width: 64, height: 64, borderRadius: "50%", background: "var(--danger-soft)",
          color: "var(--danger)", display: "flex", alignItems: "center", justifyContent: "center",
        }}
      >
        <Icon name="lock" size={28} />
      </div>
      <h1 style={{ fontSize: 26 }}>상담 이용이 제한되어 있습니다</h1>
      <p className="muted" style={{ fontSize: 15, lineHeight: 1.7 }}>
        상담 중 폭언으로 상담이 강제 종료되어, 상담원 보호 정책에 따라 일정
        기간 새 상담을 시작할 수 없습니다.
      </p>

      <div style={{ display: "flex", gap: 12, flexWrap: "wrap", justifyContent: "center" }}>
        <div className="wait-stat" style={{ background: "var(--danger-soft)", borderColor: "#f2c4be" }}>
          <span>해제 예정</span>
          <b>{valid ? formatDateTime(releaseAt) : "-"}</b>
        </div>
        <div className="wait-stat">
          <span>남은 기간</span>
          <b>{valid ? daysLeft(releaseAt) : "-"}</b>
        </div>
      </div>

      <div style={{ width: "100%", textAlign: "left" }}>
        <div className="kv"><span>사유</span><span>{reason || "-"}</span></div>
      </div>

      <button type="button" className="btn btn-lg" disabled style={{ width: "100%" }}>
        상담 시작 (이용정지 중)
      </button>
      <span className="small muted">
        해제 일시가 지나면 다시 상담을 시작할 수 있습니다.
      </span>
    </section>
  );
}
