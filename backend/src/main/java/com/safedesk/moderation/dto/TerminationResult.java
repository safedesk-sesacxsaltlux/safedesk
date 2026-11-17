package com.safedesk.moderation.dto;

import com.safedesk.chat.entity.CloseReason;
import com.safedesk.chat.entity.ClosedBy;
import com.safedesk.chat.entity.ConsultationStatus;
import com.safedesk.sanction.dto.SanctionResult;

import java.time.LocalDateTime;

/**
 * 폭언 누적 자동 종료 결과 (docs/api/moderation.md MOD-03).
 * ChatService 가 이 값으로 종료 알림(CONSULTATION_TERMINATED)을 만든다.
 *
 * @param consultationId 종료된 상담 ID
 * @param status         TERMINATED
 * @param closeReason    ABUSE_AUTO
 * @param closedBy       SYSTEM
 * @param closedAt       상담 종료 시각 = 정지 시작 시각 (REQ-021)
 * @param suspendDays    정지 일수 (MOD-04 결과)
 * @param suspendEndAt   정지 해제 예정 일시 (MOD-04 결과의 endAt)
 */
public record TerminationResult(
    Long consultationId,
    String status,
    String closeReason,
    String closedBy,
    LocalDateTime closedAt,
    int suspendDays,
    LocalDateTime suspendEndAt
) {

    /**
     * 종료 정보와 MOD-04 결과로 만든다. 상태·사유·주체는 폭언 자동 종료라 항상 같은 값이다.
     */
    public static TerminationResult of(Long consultationId, LocalDateTime closedAt, SanctionResult sanctionResult) {
        return new TerminationResult(
            consultationId,
            ConsultationStatus.TERMINATED,
            CloseReason.ABUSE_AUTO,
            ClosedBy.SYSTEM,
            closedAt,
            sanctionResult.suspendDays(),
            sanctionResult.endAt()
        );
    }
}
