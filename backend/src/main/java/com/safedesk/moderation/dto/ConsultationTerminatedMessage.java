package com.safedesk.moderation.dto;

import java.time.LocalDateTime;

/**
 * WebSocket /topic/chats/{consultationId} 로 발행하는 폭언 종료 알림 (contract.md WebSocket 「CONSULTATION_TERMINATED」).
 *
 * {
 *   "type": "CONSULTATION_TERMINATED",
 *   "consultationId": 101,
 *   "status": "TERMINATED",
 *   "reason": "반복된 폭언으로 상담이 종료되었습니다. 7일간 상담을 이용할 수 없습니다.",
 *   "closedAt": "2026-11-11T13:20:00",
 *   "suspendDays": 7,
 *   "suspendEndAt": "2026-11-18T13:20:00"
 * }
 *
 * ⚠️ 원문은 절대 넣지 않는다 (NFR-005). 같은 토픽을 상담원도 구독한다.
 */
public class ConsultationTerminatedMessage {

    private final String type = "CONSULTATION_TERMINATED";
    private Long consultationId;
    private String status;
    private String reason;
    private LocalDateTime closedAt;
    private int suspendDays;
    private LocalDateTime suspendEndAt;

    public static ConsultationTerminatedMessage from(TerminationResult result) {
        ConsultationTerminatedMessage dto = new ConsultationTerminatedMessage();
        dto.consultationId = result.consultationId();
        dto.status = result.status();
        dto.reason = buildReason(result.suspendDays());
        dto.closedAt = result.closedAt();
        dto.suspendDays = result.suspendDays();
        dto.suspendEndAt = result.suspendEndAt();

        return dto;
    }

    /**
     * 종료 안내 문구 (policy.md 6장). 기본 정책 첫 위반이면 "… 7일간 상담을 이용할 수 없습니다."
     * 누적 횟수나 관리자 정책 변경에 따라 일수만 바뀐다.
     */
    private static String buildReason(int suspendDays) {
        return "반복된 폭언으로 상담이 종료되었습니다. " + suspendDays + "일간 상담을 이용할 수 없습니다.";
    }

    public String getType() {
        return type;
    }

    public Long getConsultationId() {
        return consultationId;
    }

    public String getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public int getSuspendDays() {
        return suspendDays;
    }

    public LocalDateTime getSuspendEndAt() {
        return suspendEndAt;
    }
}
