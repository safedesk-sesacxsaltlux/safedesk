package com.safedesk.history.agent.dto;

import java.time.LocalDateTime;

public record AgentConsultationResponseDTO(
    // HIS-04 내가 맡은 상담 목록의 한 줄 (docs/api/history.md)
    long consultationId,
    String consultationNo,
    String customerLoginId,
    LocalDateTime startedAt,
    LocalDateTime closedAt,
    String inquirySummary,
    long abuseCount,
    String status
) {
}
