"use client";

// 상담 상세 (HIS-06 GET /api/consultations/{id}/history). 관리자·상담원 상세 화면이 같이 쓴다.
// 조회 범위(본인 상담만 등)와 원문/마스킹본은 서버가 역할에 따라 정한다.

import Link from "next/link";
import { useApiGet } from "../_lib/useApiGet";
import {
  RESULT_LABEL,
  closeReasonLabel,
  formatDateTime,
  formatTime,
  statusBadge,
} from "../_lib/format";

const ERROR_MESSAGE = {
  CHAT_NOT_FOUND: "상담을 찾을 수 없습니다.",
  CHAT_ACCESS_DENIED: "이 상담을 볼 권한이 없습니다.",
  CHAT_NOT_CLOSED: "진행 중인 상담입니다. 상담 화면에서 확인해주세요.",
};

export default function ConsultationDetail({ id, backHref, backLabel, showSanctionLink = false }) {
  const result = useApiGet(`/api/consultations/${id}/history`);
  const state = result.status === "error"
    ? { ...result, message: ERROR_MESSAGE[result.errorCode] ?? result.message }
    : result;

  if (state.status === "loading") {
    return <span className="small muted">상담 내용을 불러오는 중입니다.</span>;
  }
  if (state.status === "error") {
    return (
      <div className="stack gap-12">
        <div className="breadcrumb"><Link href={backHref}>{backLabel}</Link></div>
        <span className="msg-err">{state.message}</span>
      </div>
    );
  }

  const { consultation: c, summary, messages } = state.data;
  const badge = statusBadge(c.status);

  return (
    <>
      <div className="breadcrumb">
        <Link href={backHref}>{backLabel}</Link> › {c.consultationNo}
      </div>

      <div className="row">
        <div className="stack gap-20" style={{ flex: "1 1 340px" }}>
          <section className="card stack gap-8">
            <div className="between" style={{ flexWrap: "nowrap" }}>
              <h1 style={{ fontSize: 20 }}>{c.consultationNo}</h1>
              <span className={`badge ${badge.tone}`}>{badge.label}</span>
            </div>
            <div className="kv"><span>고객</span><span>{c.customerLoginId ?? "—"}</span></div>
            <div className="kv"><span>상담원</span><span>{c.agentName ?? "AI 응대"}</span></div>
            <div className="kv"><span>시작</span><span>{formatDateTime(c.startedAt)}</span></div>
            <div className="kv"><span>종료</span><span>{formatDateTime(c.closedAt)}</span></div>
            <div className="kv"><span>종료 사유</span><span>{closeReasonLabel(c.closeReason, c.closedBy)}</span></div>
            <div className="kv"><span>폭언 감지</span><span>{c.abuseCount}회</span></div>
            {c.violationRecorded && (
              <div className="kv"><span>위반 기록</span><span className="badge badge-danger">위반 1회 누적</span></div>
            )}
          </section>

          <section className="card stack gap-12">
            <div className="stack gap-4">
              <h2>상담 요약</h2>
              <span className="small muted">규칙 기반 템플릿 자동 생성</span>
            </div>
            {summary.summaryStatus === "DONE" ? (
              <div className="transcript">
                <div><b>문의 유형</b><br /><span className="muted">{summary.inquiryCategory ?? "—"}</span></div>
                <div><b>문의 내용</b><br /><span className="muted">{summary.inquirySummary ?? "—"}</span></div>
                <div><b>처리 내용</b><br /><span className="muted">{summary.processSummary}</span></div>
                <div><b>처리 결과</b><br /><span className="muted">{RESULT_LABEL[summary.result] ?? "—"}</span></div>
              </div>
            ) : (
              <span className="small muted">요약 없음</span>
            )}
          </section>
        </div>

        <section className="card stack gap-12" style={{ flex: "2 1 520px", minWidth: 0 }}>
          <div className="between">
            <h2>메시지 내역</h2>
            <span className="small muted">
              폭언 메시지는 마스킹되어 표시됩니다
              {showSanctionLink && (
                <> · <Link href="/admin/sanctions">원문은 제재 상세에서 열람</Link></>
              )}
            </span>
          </div>
          <Timeline consultation={c} messages={messages} />
        </section>
      </div>
    </>
  );
}

// 메시지 목록 + 구간 구분선(AI 상담 / 상담원 연결 / 종료). 구분선은 queuedAt·acceptedAt·closedAt 으로 그린다 (명세)
function Timeline({ consultation: c, messages }) {
  if (messages.length === 0) {
    return <span className="small muted">메시지가 없습니다.</span>;
  }

  const items = [];
  let agentDividerShown = false;
  items.push(<div key="d-ai" className="tl-divider">AI 상담 구간</div>);

  messages.forEach((m) => {
    if (!agentDividerShown && c.acceptedAt && m.createdAt >= c.acceptedAt) {
      items.push(<div key="d-agent" className="tl-divider">상담원 연결 · {formatTime(c.acceptedAt)}</div>);
      agentDividerShown = true;
    }
    items.push(<MessageItem key={m.messageId} message={m} />);
  });

  const terminated = c.status === "TERMINATED";
  items.push(
    <div
      key="d-end"
      className="tl-divider"
      style={terminated ? { background: "var(--danger-soft)", color: "var(--danger)" } : undefined}
    >
      {closeReasonLabel(c.closeReason, c.closedBy)} · {formatTime(c.closedAt)}
    </div>,
  );

  return <div className="timeline">{items}</div>;
}

function MessageItem({ message: m }) {
  const abusive = m.abuseStatus === "ABUSIVE";
  const style = abusive
    ? m.warningNo && m.warningNo >= 2
      ? { background: "var(--danger-soft)", borderColor: "#f2c4be" }
      : { background: "var(--warn-soft)", borderColor: "#f0cfa8" }
    : undefined;

  return (
    <div className="tl-item" style={style}>
      <b>{m.senderName ?? m.senderType}</b> · {formatTime(m.createdAt)}
      {abusive && (
        <span className={`badge ${m.warningNo >= 2 ? "badge-danger" : "badge-warn"}`} style={{ marginLeft: 6 }}>
          ABUSIVE{m.warningNo ? ` · 경고 ${m.warningNo}` : ""}
        </span>
      )}
      <br />
      <span className={abusive ? "muted" : undefined} style={{ whiteSpace: "pre-wrap" }}>{m.content}</span>
    </div>
  );
}
