package com.safedesk.history.agent;

import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.history.agent.dto.AgentCustomerHistoryResponseDTO;
import com.safedesk.history.agent.dto.AgentCustomerHistoryResponseDTO.CustomerRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * HIS-03 응대 고객별 이력. 검색 조건 변환, 기간 검증, 응답 가공을 본다.
 * (최근 상담 고르기·집계 쿼리는 실제 DB 로 따로 확인)
 */
class AgentCustomerServiceTest {

    private static final long AGENT_ID = 2L;

    private AgentCustomerRepository repository;
    private AgentCustomerService service;

    @BeforeEach
    void setUp() {
        repository = mock(AgentCustomerRepository.class);
        service = new AgentCustomerService(repository);
        when(repository.stats(any(), any(), any())).thenReturn(stats(0L, 0L, null, null));
        when(repository.searchCustomers(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
    }

    @Test
    @DisplayName("조건이 없으면 상담원 ID 만 걸고 나머지는 null(조건 없음)")
    void noFilter() {
        service.search(AGENT_ID, null, null, null, null, 0, 20);

        verify(repository).stats(eq(AGENT_ID), isNull(), isNull());
        verify(repository).searchCustomers(eq(AGENT_ID), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(PageRequest.of(0, 20)));
    }

    @Test
    @DisplayName("카드(stats)는 기간만, 목록은 기간·상태·키워드를 따른다")
    void statsFollowPeriodOnly() {
        service.search(AGENT_ID, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), "TERMINATED", "C-000002", 0, 20);

        LocalDateTime from = LocalDateTime.of(2026, 11, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 12, 1, 0, 0);
        verify(repository).stats(eq(AGENT_ID), eq(from), eq(to));
        verify(repository).searchCustomers(eq(AGENT_ID), eq(from), eq(to), eq("TERMINATED"),
                eq(2L), eq("%C-000002%"), any());
    }

    @Test
    @DisplayName("고객 아이디 키워드는 상담번호 없이 아이디로만 찾는다")
    void keywordLoginId() {
        service.search(AGENT_ID, null, null, "ALL", " user07 ", 0, 20);

        verify(repository).searchCustomers(eq(AGENT_ID), isNull(), isNull(), isNull(),
                isNull(), eq("%user07%"), any());
    }

    @Test
    @DisplayName("기간: from 이 to 보다 뒤거나 366일을 넘으면 400 INVALID_PERIOD")
    void invalidPeriod() {
        assertErrorCode(() -> service.search(AGENT_ID, LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 1),
                null, null, 0, 20), ErrorCode.INVALID_PERIOD);
        // 2026-01-01 ~ 2027-01-02 = 367일
        assertErrorCode(() -> service.search(AGENT_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 2),
                null, null, 0, 20), ErrorCode.INVALID_PERIOD);
    }

    @Test
    @DisplayName("기간이 정확히 366일이면 허용한다")
    void periodBoundary() {
        // 2026-01-01 ~ 2027-01-01 = 366일
        service.search(AGENT_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), null, null, 0, 20);

        verify(repository).stats(eq(AGENT_ID), any(), any());
    }

    @Test
    @DisplayName("잘못된 status·page·size 는 400 INVALID_INPUT")
    void invalidInput() {
        assertErrorCode(() -> service.search(AGENT_ID, null, null, "QUEUED", null, 0, 20), ErrorCode.INVALID_INPUT);
        assertErrorCode(() -> service.search(AGENT_ID, null, null, null, null, -1, 20), ErrorCode.INVALID_INPUT);
        assertErrorCode(() -> service.search(AGENT_ID, null, null, null, null, 0, 51), ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("카드: 집계값이 null 이면 0, 평균 상담 시간은 초 단위로 반올림")
    void stats() {
        when(repository.stats(any(), any(), any())).thenReturn(stats(4L, 4L, new BigDecimal("337.5"), new BigDecimal("2")));

        AgentCustomerHistoryResponseDTO.Stats s = service.search(AGENT_ID, null, null, null, null, 0, 20).stats();

        assertThat(s.servedCustomerCount()).isEqualTo(4);
        assertThat(s.totalConsultationCount()).isEqualTo(4);
        assertThat(s.avgDurationSeconds()).isEqualTo(338);
        assertThat(s.abuseTerminatedCount()).isEqualTo(2);

        when(repository.stats(any(), any(), any())).thenReturn(stats(0L, 0L, null, null));
        AgentCustomerHistoryResponseDTO.Stats empty = service.search(AGENT_ID, null, null, null, null, 0, 20).stats();
        assertThat(empty.avgDurationSeconds()).isZero();
        assertThat(empty.abuseTerminatedCount()).isZero();
    }

    @Test
    @DisplayName("목록: 위반 이력 1/0 → true/false, 최근 상담은 상담번호 형식으로")
    void customerRow() {
        LocalDateTime started = LocalDateTime.of(2026, 9, 25, 10, 30);
        when(repository.searchCustomers(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        row(8L, "user04", 2, 1, 2L, started, "보조금 지급 지연", "TERMINATED", 1),
                        row(11L, "user07", 1, 0, 9L, started, "단수 보상", "CLOSED", 0)
                ), PageRequest.of(0, 2), 3));

        var customers = service.search(AGENT_ID, null, null, null, null, 0, 2).customers();

        CustomerRow first = customers.content().get(0);
        assertThat(first.customerLoginId()).isEqualTo("user04");
        assertThat(first.hasViolationHistory()).isTrue();
        assertThat(first.myConsultationCount()).isEqualTo(2);
        assertThat(first.latestConsultation().consultationNo()).isEqualTo("C-000002");
        assertThat(first.latestConsultation().abuseCount()).isEqualTo(1);
        assertThat(customers.content().get(1).hasViolationHistory()).isFalse();
        assertThat(customers.hasNext()).isTrue();
    }

    // ---- 준비용 도우미 (native 결과 인터페이스를 테스트용으로 구현) ----

    private static AgentCustomerStatsRow stats(Number served, Number total, Number avg, Number terminated) {
        return new AgentCustomerStatsRow() {
            public Number getServedCustomerCount() { return served; }
            public Number getTotalConsultationCount() { return total; }
            public Number getAvgDurationSeconds() { return avg; }
            public Number getAbuseTerminatedCount() { return terminated; }
        };
    }

    private static AgentCustomerRow row(long customerId, String loginId, long count, long violation,
                                        long consultationId, LocalDateTime started, String summary,
                                        String status, long abuse) {
        return new AgentCustomerRow() {
            public Long getCustomerId() { return customerId; }
            public String getCustomerLoginId() { return loginId; }
            public Number getMyConsultationCount() { return count; }
            public Number getHasViolationHistory() { return violation; }
            public Long getConsultationId() { return consultationId; }
            public LocalDateTime getStartedAt() { return started; }
            public String getInquirySummary() { return summary; }
            public String getStatus() { return status; }
            public Number getAbuseCount() { return abuse; }
        };
    }

    private static void assertErrorCode(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
