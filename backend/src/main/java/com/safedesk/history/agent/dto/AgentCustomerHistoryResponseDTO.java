package com.safedesk.history.agent.dto;
import com.safedesk.common.response.PageResponse;
import java.time.LocalDateTime;
// HIS-03 응대 고객별 이력 응답 (docs/api/history.md). { stats, customers } 두 덩어리
public record AgentCustomerHistoryResponseDTO(
    Stats stats,
    PageResponse<CustomerRow> customers
) {
    // 상단 카드 4개 (기간 필터만 따른다)
    public record Stats(
        long servedCustomerCount,       // 응대한 고객 수
        long totalConsultationCount,    // 총 상담 건수
        long avgDurationSeconds,        // 평균 상담 시간(초): 상담원 수락 ~ 종료
        long abuseTerminatedCount       // 강제 종료(TERMINATED) 상담 수
    ) {
    }
    // 고객 한 명 = 한 줄
    public record CustomerRow(
        long customerId,
        String customerLoginId,
        boolean hasViolationHistory,    // 위반 이력 배지
        long myConsultationCount,       // 이 고객과 나의 상담 수
        LatestConsultation latestConsultation
    ) {
    }
    // 그 고객과 나의 가장 최근 상담
    public record LatestConsultation(
        long consultationId,
        String consultationNo,          // "C-000123"
        LocalDateTime startedAt,
        String inquirySummary,
        long abuseCount,
        String status
    ) {
    }
}
