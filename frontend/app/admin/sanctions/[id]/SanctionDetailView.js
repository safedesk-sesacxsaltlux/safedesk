"use client";

// 관리자 제재 상세 (API-ADM-05 GET /api/admin/sanctions/{sanctionId})

import Link from "next/link";
import { useApiGet } from "../../../_lib/useApiGet";
import { formatDate, formatDateTime } from "../../../_lib/format";

const STATUS_BADGE = {
  SUSPENDED: { label: "정지 중", tone: "badge-danger" },
  RELEASED: { label: "해제됨", tone: "badge-ok" },
  EXPIRED: { label: "기간 만료", tone: "" },
};

const CLOSED_BY_LABEL = {
  SYSTEM: "반복 폭언으로 자동 종료",
  AGENT: "폭언으로 상담원 종료",
};

const ERROR_MESSAGE = {
  SANCTION_NOT_FOUND: "제재 정보를 찾을 수 없습니다.",
};

function consultationNo(id) {
  return `C-${String(id).padStart(6, "0")}`;
}

export default function SanctionDetailView({ id }) {
  const result = useApiGet(`/api/admin/sanctions/${id}`);

  if (result.status === "loading") {
    return <span className="small muted">제재 정보를 불러오는 중입니다.</span>;
  }
  if (result.status === "error") {
    return (
      <div className="stack gap-12">
        <div className="breadcrumb"><Link href="/admin/sanctions">제재 관리</Link></div>
        <span className="msg-err">{ERROR_MESSAGE[result.errorCode] ?? result.message}</span>
      </div>
    );
  }

  const s = result.data;
  const badge = STATUS_BADGE[s.status] ?? { label: s.status, tone: "" };

  return (
    <>
      <div className="breadcrumb">
        <Link href="/admin/sanctions">제재 관리</Link> › {s.customer.loginId}
      </div>

      <div className="row">
        <section className="card stack gap-12" style={{ flex: "1 1 300px" }}>
          <div className="between" style={{ flexWrap: "nowrap" }}>
            <h1 style={{ fontSize: 22 }}>{s.customer.loginId}</h1>
            <span className={`badge ${badge.tone}`}>{badge.label}</span>
          </div>
          <div className="kv"><span>이름</span><span>{s.customer.name}</span></div>
          <div className="kv"><span>누적 위반</span><b>{s.violationCount}회</b></div>
          <div className="kv"><span>제재</span><span>{s.durationDays}일 이용정지</span></div>
          <div className="kv"><span>기간</span><span>{formatDate(s.startsAt)} ~ {formatDate(s.endsAt)}</span></div>
          <div className="kv"><span>가입일</span><span>{formatDate(s.customer.joinedAt)}</span></div>

          {s.releasable && (
            <>
              <button type="button" className="btn btn-danger">제재 해제</button>
              <span className="small muted">해제해도 위반 이력은 보존됩니다.</span>
            </>
          )}

          {s.release && (
            <div className="stack gap-4" style={{ borderTop: "1px solid var(--line)", paddingTop: 12 }}>
              <div className="kv"><span>해제 일시</span><span>{formatDateTime(s.release.releasedAt)}</span></div>
              <div className="kv"><span>해제한 관리자</span><span>{s.release.releasedBy?.name ?? "—"}</span></div>
              <div className="kv"><span>해제 사유</span><span>{s.release.reason}</span></div>
            </div>
          )}
        </section>

        <section className="card stack gap-12" style={{ flex: "2 1 520px", minWidth: 0 }}>
          <h2>위반 이력</h2>
          <div className="timeline">
            {s.violations.map((v) => (
              <div key={v.violationId} className="tl-item stack gap-4">
                <span className="muted small">{formatDateTime(v.violatedAt)}</span>
                <span>
                  위반 {v.sequence}회차 · 상담{" "}
                  <Link href={`/admin/history/${v.consultationId}`}>{consultationNo(v.consultationId)}</Link>{" "}
                  {CLOSED_BY_LABEL[v.closedBy] ?? v.closedBy}
                </span>
                <span className="small muted">
                  폭언 메시지 {v.abusiveMessageCount}건
                  {v.appliedDurationDays && ` · ${v.appliedDurationDays}일 정지 적용`}
                </span>
              </div>
            ))}
          </div>
        </section>
      </div>
    </>
  );
}
