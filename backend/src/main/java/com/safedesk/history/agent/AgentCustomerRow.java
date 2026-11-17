package com.safedesk.history.agent;
import java.time.LocalDateTime;
// 고객별 목록 한 줄 (native 쿼리 결과). getter 이름과 SQL "as 별칭"이 같아야 담긴다.
// 숫자는 MySQL 이 돌려주는 타입이 경우마다 달라서(Long, Integer, BigDecimal) Number 로 받고 서비스에서 바꾼다.
public interface AgentCustomerRow {
    Long getCustomerId();
    String getCustomerLoginId();
    Number getMyConsultationCount();
    Number getHasViolationHistory();     // 위반이 있으면 1, 없으면 0
    Long getConsultationId();            // 아래는 모두 "최근 상담" 의 값
    LocalDateTime getStartedAt();
    String getInquirySummary();
    String getStatus();
    Number getAbuseCount();
}
