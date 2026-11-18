"use client";

// 폭언 원문 확인 모달 (API-ADM-07 GET /api/admin/violations/{violationId}/original)

import { useApiGet } from "../../../_lib/useApiGet";
import { formatDateTime, formatTime } from "../../../_lib/format";

const DETECTED_BY_LABEL = {
  KEYWORD: "금칙어 감지",
  AGENT: "상담원 경고",
};

const ERROR_MESSAGE = {
  VIOLATION_NOT_FOUND: "위반 정보를 찾을 수 없습니다.",
};

export default function OriginalModal({ violationId, sequence, onClose }) {
  const state = useApiGet(`/api/admin/violations/${violationId}/original`);

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="original-title"
      style={{
        position: "fixed", inset: 0, zIndex: 100, padding: 16,
        background: "rgba(0, 0, 0, 0.4)", display: "flex", alignItems: "center", justifyContent: "center",
      }}
    >
      <div className="modal-box" style={{ maxWidth: 560, maxHeight: "85vh", overflowY: "auto" }}>
        <div className="between">
          <h3 id="original-title" style={{ fontSize: 18 }}>폭언 원문 (위반 {sequence}회차)</h3>
          <span className="small muted">관리자 전용</span>
        </div>

        {state.status === "loading" && <span className="small muted">원문을 불러오는 중입니다.</span>}
        {state.status === "error" && (
          <span className="msg-err">{ERROR_MESSAGE[state.errorCode] ?? state.message}</span>
        )}

        {state.status === "ready" && (
          <>
            <span className="small muted">
              {state.data.customerLoginId} · 위반 {formatDateTime(state.data.violatedAt)}
            </span>
            {state.data.messages.length === 0 && <span className="small muted">저장된 원문이 없습니다.</span>}
            <div className="timeline">
              {state.data.messages.map((m) => (
                <div key={m.messageId} className="tl-item stack gap-4"
                  style={{ background: "var(--danger-soft)", borderColor: "#f2c4be" }}>
                  <span className="small muted">
                    {formatTime(m.sentAt)}
                    {m.warningOrder && ` · 경고 ${m.warningOrder}`}
                    {` · ${DETECTED_BY_LABEL[m.detectedBy] ?? m.detectedBy}`}
                  </span>
                  <span style={{ whiteSpace: "pre-wrap" }}>{m.originalContent}</span>
                  <span className="small muted">고객에게 보인 문장: {m.maskedContent}</span>
                </div>
              ))}
            </div>
            <span className="small muted">열람 시각 {formatDateTime(state.data.viewedAt)} · 열람 기록이 남습니다</span>
          </>
        )}

        <div style={{ display: "flex", justifyContent: "flex-end" }}>
          <button type="button" className="btn" onClick={onClose}>닫기</button>
        </div>
      </div>
    </div>
  );
}
