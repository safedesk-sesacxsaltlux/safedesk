package com.safedesk.dashboard;
import java.time.LocalDateTime;
// HIS-07 대시보드 native 쿼리 결과를 받는 그릇들 (getter 이름 = SQL "as 별칭")
// 숫자는 MySQL 이 count → Long, sum → BigDecimal 로 돌려줘서 Number 로 받고 서비스에서 바꾼다.
public final class DashboardRows {
    private DashboardRows() {
    }
    // 요약 카드 (전체 상담·폭언 감지·자동 종료·신규 제재·현재 정지)
    public interface CardRow {
        Number getTotalConsultationCount();
        Number getAbuseDetectedCount();
        Number getAutoTerminatedCount();
        Number getNewSanctionCount();
        Number getCurrentSuspendedCustomerCount();
    }
    // 종료 유형 4종 + 전체 종료 수
    public interface CloseTypeRow {
        Number getTotalClosedCount();
        Number getAiClosedCount();
        Number getAgentClosedCount();
        Number getQueueCanceledCount();
        Number getTerminatedCount();
    }
    // 일별 건수 한 줄. 날짜는 "2026-11-06" 문자열로 받는다 (DATE 타입 변환 문제를 피하려고)
    public interface DailyCountRow {
        String getDay();
        Number getCnt();
    }
    // 최근 제재 한 줄
    public interface RecentSanctionRow {
        Long getSanctionId();
        Long getCustomerId();
        String getCustomerLoginId();
        Number getViolationCount();
        Number getSuspendDays();
        LocalDateTime getEndAt();
        LocalDateTime getReleasedAt();
    }
}
