package com.safedesk.history.customer;

import java.time.LocalDateTime;

// HIS-01 검색 쿼리가 돌려주는 한 줄 (DB 값 그대로). JPQL "select new" 의 순서·타입과 같아야 한다.
public record CustomerConsultationRow(
        Long consultationId,
        LocalDateTime startedAt,
        LocalDateTime closedAt,
        Long agentId,               // null 이면 AI 단독
        String status,
        String inquirySummary,
        String inquiryCategory,
        String processSummary,
        String result,
        Long violationCount         // 이 상담으로 생긴 위반 수 (0 또는 1)
) {
}
