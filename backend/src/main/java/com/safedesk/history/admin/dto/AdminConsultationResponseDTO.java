package com.safedesk.history.admin.dto;

import java.time.LocalDateTime;

public record AdminConsultationResponseDTO(
    long consultationId,
    String consultationNo,
    LocalDateTime startedAt,
    long customerId,
    String customerLoginId,
    Long agentId,
    String agentName,
    String handledBy,
    long abuseCount,
    String status
) {
}
