package com.safedesk.dashboard;

import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.dashboard.DashboardRows.CardRow;
import com.safedesk.dashboard.DashboardRows.CloseTypeRow;
import com.safedesk.dashboard.DashboardRows.DailyCountRow;
import com.safedesk.dashboard.DashboardRows.RecentSanctionRow;
import com.safedesk.dashboard.dto.DashboardResponseDTO;
import com.safedesk.dashboard.dto.DashboardResponseDTO.DailyTrend;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * HIS-07 운영 대시보드. 기간 계산, 빈 날 채우기, 비율, 제재 상태 계산을 본다. (집계 쿼리는 실제 DB 로 따로 확인)
 */
class DashboardServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 17);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 17, 19, 0);

    private DashboardRepository repository;
    private DashboardService service;

    @BeforeEach
    void setUp() {
        repository = mock(DashboardRepository.class);
        service = new DashboardService(repository);
        when(repository.cards(any(), any(), any())).thenReturn(cards(0, 0, 0, 0, 0));
        when(repository.closeTypes(any(), any())).thenReturn(closeTypes(0, null, null, null, null));
        when(repository.dailyConsultations(any(), any())).thenReturn(List.of());
        when(repository.dailyAbuse(any(), any())).thenReturn(List.of());
        when(repository.recentSanctions()).thenReturn(List.of());
    }

    @Test
    @DisplayName("기간: 기본 TODAY 는 오늘 하루, 7D·30D 는 오늘 포함 7일·30일")
    void period() {
        DashboardResponseDTO today = service.getDashboard(null, TODAY, NOW);
        assertThat(today.period().type()).isEqualTo("TODAY");
        assertThat(today.period().from()).isEqualTo(TODAY);
        verify(repository).cards(eq(TODAY.atStartOfDay()), eq(TODAY.plusDays(1).atStartOfDay()), eq(NOW));

        DashboardResponseDTO week = service.getDashboard("7D", TODAY, NOW);
        assertThat(week.period().from()).isEqualTo(LocalDate.of(2026, 11, 11));
        assertThat(week.period().to()).isEqualTo(TODAY);

        DashboardResponseDTO month = service.getDashboard("30D", TODAY, NOW);
        assertThat(month.period().from()).isEqualTo(LocalDate.of(2026, 10, 19));
    }

    @Test
    @DisplayName("잘못된 period 는 400 INVALID_INPUT")
    void invalidPeriod() {
        assertThatThrownBy(() -> service.getDashboard("1Y", TODAY, NOW))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("일별 추이: 건수가 없는 날도 0 으로 채워서 기간 일수만큼 돌려준다")
    void dailyTrendFillsEmptyDays() {
        when(repository.dailyConsultations(any(), any())).thenReturn(List.of(daily("2026-11-13", 3)));
        when(repository.dailyAbuse(any(), any())).thenReturn(List.of(daily("2026-11-15", 2)));

        List<DailyTrend> trend = service.getDashboard("7D", TODAY, NOW).dailyTrend();

        assertThat(trend).hasSize(7);
        assertThat(trend.get(0).date()).isEqualTo(LocalDate.of(2026, 11, 11));
        assertThat(trend.get(6).date()).isEqualTo(TODAY);
        assertThat(trend.get(2)).isEqualTo(new DailyTrend(LocalDate.of(2026, 11, 13), 3, 0));
        assertThat(trend.get(4)).isEqualTo(new DailyTrend(LocalDate.of(2026, 11, 15), 0, 2));
        assertThat(trend.get(1)).isEqualTo(new DailyTrend(LocalDate.of(2026, 11, 12), 0, 0));
    }

    @Test
    @DisplayName("비율: 소수점 한 자리, AI 단독 해결률 = AI 종료 ÷ 전체 종료")
    void rates() {
        when(repository.closeTypes(any(), any())).thenReturn(closeTypes(11,
                new BigDecimal("3"), new BigDecimal("2"), new BigDecimal("1"), new BigDecimal("5")));

        DashboardResponseDTO result = service.getDashboard("30D", TODAY, NOW);

        assertThat(result.cards().aiOnlyResolvedRate()).isEqualTo(27.3);
        assertThat(result.closeTypes().aiClosed().rate()).isEqualTo(27.3);
        assertThat(result.closeTypes().agentClosed().rate()).isEqualTo(18.2);
        assertThat(result.closeTypes().queueCanceled().rate()).isEqualTo(9.1);
        assertThat(result.closeTypes().terminated().count()).isEqualTo(5);
        assertThat(result.closeTypes().terminated().rate()).isEqualTo(45.5);
    }

    @Test
    @DisplayName("종료 상담이 없으면 비율은 null (화면: 집계할 데이터가 없습니다)")
    void noDataRateIsNull() {
        DashboardResponseDTO result = service.getDashboard(null, TODAY, NOW);

        assertThat(result.cards().aiOnlyResolvedRate()).isNull();
        assertThat(result.closeTypes().totalClosedCount()).isZero();
        assertThat(result.closeTypes().terminated().count()).isZero();
        assertThat(result.closeTypes().terminated().rate()).isNull();
    }

    @Test
    @DisplayName("최근 제재 상태: 해제됐으면 RELEASED, 끝났으면 EXPIRED, 아니면 SUSPENDED")
    void sanctionStatus() {
        when(repository.recentSanctions()).thenReturn(List.of(
                sanction(7, NOW.plusDays(10), null),
                sanction(6, NOW.plusDays(5), NOW.minusDays(1)),
                sanction(5, NOW.minusDays(1), null)
        ));

        var sanctions = service.getDashboard(null, TODAY, NOW).recentSanctions();

        assertThat(sanctions).extracting(DashboardResponseDTO.RecentSanction::status)
                .containsExactly("SUSPENDED", "RELEASED", "EXPIRED");
    }

    // ---- 준비용 도우미 (native 결과 인터페이스를 테스트용으로 구현) ----

    private static CardRow cards(long total, long abuse, long auto, long sanction, long suspended) {
        return new CardRow() {
            public Number getTotalConsultationCount() { return total; }
            public Number getAbuseDetectedCount() { return abuse; }
            public Number getAutoTerminatedCount() { return auto; }
            public Number getNewSanctionCount() { return sanction; }
            public Number getCurrentSuspendedCustomerCount() { return suspended; }
        };
    }

    // sum() 은 대상이 없으면 null 이 오므로 null 도 넣어 본다
    private static CloseTypeRow closeTypes(long total, Number ai, Number agent, Number canceled, Number terminated) {
        return new CloseTypeRow() {
            public Number getTotalClosedCount() { return total; }
            public Number getAiClosedCount() { return ai; }
            public Number getAgentClosedCount() { return agent; }
            public Number getQueueCanceledCount() { return canceled; }
            public Number getTerminatedCount() { return terminated; }
        };
    }

    private static DailyCountRow daily(String day, long cnt) {
        return new DailyCountRow() {
            public String getDay() { return day; }
            public Number getCnt() { return cnt; }
        };
    }

    private static RecentSanctionRow sanction(long id, LocalDateTime endAt, LocalDateTime releasedAt) {
        return new RecentSanctionRow() {
            public Long getSanctionId() { return id; }
            public Long getCustomerId() { return 8L; }
            public String getCustomerLoginId() { return "user04"; }
            public Number getViolationCount() { return 1; }
            public Number getSuspendDays() { return 7; }
            public LocalDateTime getEndAt() { return endAt; }
            public LocalDateTime getReleasedAt() { return releasedAt; }
        };
    }
}
