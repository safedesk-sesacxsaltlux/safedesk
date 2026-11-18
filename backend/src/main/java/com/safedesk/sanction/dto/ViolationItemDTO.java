package com.safedesk.sanction.dto;

import com.safedesk.sanction.repository.ViolationHistoryRow;

import java.time.LocalDateTime;

public record ViolationItemDTO(
    Long violationId,
    int sequence,
    Long consultationId,
    String closedBy,
    LocalDateTime violatedAt,
    long abusiveMessageCount,
    Integer appliedDurationDays
) {

    public static ViolationItemDTO of(ViolationHistoryRow row, int sequence) {
        return new ViolationItemDTO(
            row.getViolationId(),
            sequence,
            row.getConsultationId(),
            row.getClosedBy(),
            row.getViolatedAt(),
            row.getAbusiveMessageCount(),
            row.getAppliedDurationDays()
        );
    }
}
