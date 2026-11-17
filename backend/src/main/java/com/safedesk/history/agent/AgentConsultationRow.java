package com.safedesk.history.agent;

import java.time.LocalDateTime;
// 검색 쿼리가 돌려주는 한 줄 (DB 값 그대로). JPQL "select new" 의 순서·타입과 같아야 한다.
public record AgentConsultationRow(
    Long consultationId,
    String customerLoginId,
    LocalDateTime startedAt,
    LocalDateTime closedAt,
    String inquirySummary,
    String status,
    Long abuseCount
) {
}
