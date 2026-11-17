package com.safedesk.history.agent;

import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.common.response.PageResponse;
import com.safedesk.history.agent.dto.AgentConsultationResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
 * HIS-04 내가 맡은 상담 목록. 검색 조건 변환과 응답 가공을 본다. (쿼리는 실제 DB 로 따로 확인)
 */
class AgentConsultationServiceTest {

    private static final long AGENT_ID = 4L;

    private AgentConsultationRepository repository;
    private AgentConsultationService service;

    @BeforeEach
    void setUp() {
        repository = mock(AgentConsultationRepository.class);
        service = new AgentConsultationService(repository);
        when(repository.search(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
    }

    @Test
    @DisplayName("항상 로그인한 상담원 ID 로 조회하고, 다른 조건이 없으면 null(조건 없음)")
    void alwaysFiltersByAgent() {
        service.search(AGENT_ID, null, null, null, null, 0, 20);

        verify(repository).search(eq(AGENT_ID), isNull(), isNull(), isNull(), isNull(), eq(PageRequest.of(0, 20)));
    }

    @Test
    @DisplayName("고객·상태·기간 조건을 쿼리 파라미터로 넘긴다 (to 는 다음 날 0시 미만)")
    void filters() {
        service.search(AGENT_ID, 8L, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10), "TERMINATED", 0, 20);

        verify(repository).search(eq(AGENT_ID), eq(8L), eq("TERMINATED"),
                eq(LocalDateTime.of(2026, 11, 1, 0, 0)), eq(LocalDateTime.of(2026, 11, 11, 0, 0)), any());
    }

    @Test
    @DisplayName("status=ALL 은 조건 없음")
    void statusAll() {
        service.search(AGENT_ID, null, null, null, "ALL", 0, 20);

        verify(repository).search(eq(AGENT_ID), isNull(), isNull(), isNull(), isNull(), any());
    }

    @Test
    @DisplayName("from 이 to 보다 뒤면 400 INVALID_PERIOD")
    void invalidPeriod() {
        assertErrorCode(() -> service.search(AGENT_ID, null, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1),
                null, 0, 20), ErrorCode.INVALID_PERIOD);
    }

    @Test
    @DisplayName("잘못된 status·page·size 는 400 INVALID_INPUT")
    void invalidInput() {
        assertErrorCode(() -> service.search(AGENT_ID, null, null, null, "QUEUED", 0, 20), ErrorCode.INVALID_INPUT);
        assertErrorCode(() -> service.search(AGENT_ID, null, null, null, null, -1, 20), ErrorCode.INVALID_INPUT);
        assertErrorCode(() -> service.search(AGENT_ID, null, null, null, null, 0, 51), ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("응답: 상담번호 형식, 문의 요약·종료 시각, 페이지 정보")
    void response() {
        LocalDateTime started = LocalDateTime.of(2026, 9, 10, 13, 0);
        when(repository.search(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        new AgentConsultationRow(1L, "user04", started, started.plusMinutes(8),
                                "주민센터 민원 문의", "TERMINATED", 2L)
                ), PageRequest.of(0, 1), 2));

        PageResponse<AgentConsultationResponseDTO> result = service.search(AGENT_ID, null, null, null, null, 0, 1);

        AgentConsultationResponseDTO dto = result.content().get(0);
        assertThat(dto.consultationNo()).isEqualTo("C-000001");
        assertThat(dto.customerLoginId()).isEqualTo("user04");
        assertThat(dto.closedAt()).isEqualTo(started.plusMinutes(8));
        assertThat(dto.inquirySummary()).isEqualTo("주민센터 민원 문의");
        assertThat(dto.abuseCount()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();
    }

    private static void assertErrorCode(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
