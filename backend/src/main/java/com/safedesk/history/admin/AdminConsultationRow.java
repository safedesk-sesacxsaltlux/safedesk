package com.safedesk.history.admin;

import java.time.LocalDateTime;

// 검색 쿼리가 돌려주는 한 줄 (DB 값 그대로). 화면용 가공(상담번호, handledBy)은 서비스에서 한다.
// JPQL 의 "select new ..." 가 이 생성자를 호출해서 결과를 담는다 → 필드 순서·타입이 쿼리와 같아야 함
public record AdminConsultationRow(
    Long consultationId,
    LocalDateTime startedAt,
    Long customerId,
    String customerLoginId,
    Long agentId,
    String agentName,
    String status,
    Long abuseCount
) {
}
