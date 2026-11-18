package com.safedesk.history.customer.dto;

import java.time.LocalDateTime;

// HIS-01 내 상담 이력 목록의 한 줄 (docs/api/history.md)
public record CustomerConsultationResponseDTO(
        long consultationId,
        String consultationNo,      // "C-000123"
        LocalDateTime startedAt,
        LocalDateTime closedAt,
        String handledBy,           // AI | AGENT
        String status,              // CLOSED | TERMINATED
        String inquirySummary,
        Summary summary,
        boolean violationRecorded   // true 면 화면에 "위반 1회 누적" 표시
) {

    public record Summary(
            String inquiryCategory,
            String processSummary,
            String result           // RESOLVED | UNRESOLVED | TERMINATED
    ) {
    }
}
