package com.safedesk.dashboard.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// HIS-07 운영 대시보드 응답 (docs/api/history.md)
public record DashboardResponseDTO(
    Period period,
    Cards cards,
    List<DailyTrend> dailyTrend,
    CloseTypes closeTypes,
    List<RecentSanction> recentSanctions
) {

    // 조회 기간. type 은 TODAY | 7D | 30D, from·to 는 오늘 포함
    public record Period(String type, LocalDate from, LocalDate to) {
    }

    // 요약 카드. 비율은 소수점 한 자리, 분모가 0 이면 null (화면: "집계할 데이터가 없습니다.")
    public record Cards(
        long totalConsultationCount,        // 기간 내 시작된 상담 (모든 상태)
        Double aiOnlyResolvedRate,          // AI 단독 종료 ÷ 기간 내 종료 상담 × 100
        long abuseDetectedCount,            // 기간 내 폭언 감지 메시지 수
        long autoTerminatedCount,           // 기간 내 시스템 자동 종료
        long newSanctionCount,              // 기간 내 생긴 이용정지
        long currentSuspendedCustomerCount  // 지금 정지 중인 고객 수 (기간 무관)
    ) {
    }

    // 하루 한 줄. 건수가 0 인 날도 포함
    public record DailyTrend(LocalDate date, long consultationCount, long abuseDetectedCount) {
    }

    public record CloseTypes(
        long totalClosedCount,
        CloseType aiClosed,         // CLOSED, 대기열을 거치지 않음
        CloseType agentClosed,      // CLOSED, 상담원이 수락함
        CloseType queueCanceled,    // CLOSED, 대기 중 취소
        CloseType terminated        // TERMINATED (강제 종료)
    ) {
    }

    public record CloseType(long count, Double rate) {
    }

    public record RecentSanction(
        long sanctionId,
        long customerId,
        String customerLoginId,
        long violationCount,        // 이 제재가 그 고객의 몇 번째 위반으로 생겼는지
        int suspendDays,
        LocalDateTime endAt,
        String status               // SUSPENDED | EXPIRED | RELEASED
    ) {
    }
}

