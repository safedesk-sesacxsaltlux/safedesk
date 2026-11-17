package com.safedesk.history.agent;

public interface AgentCustomerStatsRow {
    // 상단 카드 4개 (native 집계 결과). avg·sum 은 BigDecimal, count 는 Long 이라 Number 로 받는다.
// 대상 상담이 하나도 없으면 avg 는 null, sum 도 null 이 올 수 있다.
    Number getServedCustomerCount();
    Number getTotalConsultationCount();
    Number getAvgDurationSeconds();
    Number getAbuseTerminatedCount();
}
