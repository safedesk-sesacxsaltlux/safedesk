package com.safedesk.chat.dto;

import java.time.LocalDateTime;

/**
 * API-008 상담원 회복 시간 조회 응답 (REQ-033).
 * canAcceptChat 은 회복 시간과 진행 중 상담을 함께 본 값이다 (chat.md API-008 비고).
 * activeConsultationId 는 진행 중인 상담 번호 (없으면 null). 콘솔에서 채팅 화면으로 돌아갈 때 쓴다.
 */
public record AgentRecoveryResponseDTO(
    Long agentId,
    boolean isRecovering,
    LocalDateTime recoveryStartedAt,
    LocalDateTime recoveryEndsAt,
    long remainingSeconds,
    boolean canAcceptChat,
    Long activeConsultationId
) {
}
