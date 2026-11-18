"use client";

// 제재 해제 확인 모달 (API-ADM-06 POST /api/admin/sanctions/{sanctionId}/release)

import { useState } from "react";
import { apiPost } from "../../../_lib/api";

const MAX_REASON_LENGTH = 255;

export default function ReleaseModal({ sanction, onClose, onReleased }) {
  const [reason, setReason] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  async function submit(e) {
    e.preventDefault();
    if (!reason.trim()) {
      setError("해제 사유를 입력해주세요.");
      return;
    }
    setSaving(true);
    setError("");
    const result = await apiPost(`/api/admin/sanctions/${sanction.sanctionId}/release`, { reason });
    setSaving(false);
    if (!result.ok) {
      setError(result.message);
      return;
    }
    onReleased(result.message);
  }

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="release-title"
      style={{
        position: "fixed", inset: 0, zIndex: 100, padding: 16,
        background: "rgba(0, 0, 0, 0.4)", display: "flex", alignItems: "center", justifyContent: "center",
      }}
    >
      <form className="modal-box" onSubmit={submit}>
        <h3 id="release-title" style={{ fontSize: 18 }}>제재를 해제할까요?</h3>
        <p className="muted" style={{ fontSize: 14, lineHeight: 1.6 }}>
          {sanction.customer.loginId}의 {sanction.durationDays}일 이용정지가 즉시 해제되고, 고객은 바로 상담을 시작할 수
          있습니다. 위반 이력은 보존됩니다.
        </p>
        <div className="field">
          <label htmlFor="sd-reason">해제 사유 (필수)</label>
          <textarea
            id="sd-reason"
            className="input"
            rows={3}
            maxLength={MAX_REASON_LENGTH}
            placeholder="해제 사유를 입력하세요"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            autoFocus
          />
          <span className="small muted" style={{ alignSelf: "flex-end" }}>{reason.length} / {MAX_REASON_LENGTH}</span>
        </div>
        {error && <span className="msg-err">{error}</span>}
        <div style={{ display: "flex", gap: 8, justifyContent: "flex-end" }}>
          <button type="button" className="btn" onClick={onClose} disabled={saving}>취소</button>
          <button type="submit" className="btn btn-danger" disabled={saving}>
            {saving ? "해제하는 중" : "해제하기"}
          </button>
        </div>
      </form>
    </div>
  );
}
