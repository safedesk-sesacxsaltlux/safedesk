package com.safedesk.sanction.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ViolationOriginalDTO(
    Long violationId,
    Long consultationId,
    String customerLoginId,
    String closedBy,
    LocalDateTime violatedAt,
    List<ViolationMessageDTO> messages,
    LocalDateTime viewedAt
) {
}
