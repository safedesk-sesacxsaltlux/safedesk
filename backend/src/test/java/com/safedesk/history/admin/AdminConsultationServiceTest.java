package com.safedesk.history.admin;

import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.common.response.PageResponse;
import com.safedesk.history.admin.dto.AdminConsultationResponseDTO;
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
 * HIS-05 전체 상담 이력. 검색 조건이 쿼리 파라미터로 바르게 바뀌는지, 응답이 바르게 만들어지는지 본다.
 * (쿼리 자체는 실제 DB 로 따로 확인)
 */
class AdminConsultationServiceTest {

    private AdminConsultationRepository repository;
    private AdminConsultationService service;

    @BeforeEach
    void setUp() {
        repository = mock(AdminConsultationRepository.class);
        service = new AdminConsultationService(repository);
        when(repository.search(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
    }

    @Test
    @DisplayName("조건이 없으면 모든 필터를 null(조건 없음)로 넘긴다")
    void noFilter() {
        service.search(null, null, null, null, null, null, 0, 20);

        verify(repository).search(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(PageRequest.of(0, 20)));
    }

    @Test
    @DisplayName("ALL 과 빈 키워드는 조건 없음과 같다")
    void allMeansNoFilter() {
        service.search(null, null, "ALL", "ALL", null, "  ", 0, 20);

        verify(repository).search(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any());
    }

    @Test
    @DisplayName("기간: from 은 그날 0시부터, to 는 다음 날 0시 미만까지 (그날 포함)")
    void period() {
        service.search(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10), null, null, null, null, 0, 20);

        verify(repository).search(isNull(),
                eq(LocalDateTime.of(2026, 11, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 11, 11, 0, 0)),
                isNull(), isNull(), isNull(), isNull(), any());
    }

    @Test
    @DisplayName("from 이 to 보다 뒤면 400 INVALID_PERIOD")
    void invalidPeriod() {
        assertErrorCode(() -> service.search(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1),
                null, null, null, null, 0, 20), ErrorCode.INVALID_PERIOD);
    }

    @Test
    @DisplayName("handledBy: AI 는 상담원 없음(false), AGENT 는 상담원 있음(true)")
    void handledBy() {
        service.search(null, null, null, "AI", null, null, 0, 20);
        verify(repository).search(isNull(), isNull(), isNull(), eq(false), isNull(), isNull(), isNull(), any());

        service.search(null, null, null, "AGENT", null, null, 0, 20);
        verify(repository).search(isNull(), isNull(), isNull(), eq(true), isNull(), isNull(), isNull(), any());
    }

    @Test
    @DisplayName("상담번호 키워드: C-000123, c123, 123 모두 123번으로 찾고 고객 아이디로도 찾는다")
    void keywordConsultationNo() {
        for (String keyword : List.of("C-000123", "c123", "123")) {
            service.search(null, null, null, null, null, keyword, 0, 20);
            verify(repository).search(isNull(), isNull(), isNull(), isNull(), isNull(),
                    eq(123L), eq("%" + keyword + "%"), any());
        }
    }

    @Test
    @DisplayName("상담번호 모양이 아닌 키워드는 고객 아이디로만 찾는다 (앞뒤 공백 제거)")
    void keywordLoginId() {
        service.search(null, null, null, null, null, " user04 ", 0, 20);

        verify(repository).search(isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), eq("%user04%"), any());
    }

    @Test
    @DisplayName("잘못된 status·handledBy·page·size 는 400 INVALID_INPUT")
    void invalidInput() {
        assertErrorCode(() -> service.search(null, null, "QUEUED", null, null, null, 0, 20), ErrorCode.INVALID_INPUT);
        assertErrorCode(() -> service.search(null, null, null, "BOT", null, null, 0, 20), ErrorCode.INVALID_INPUT);
        assertErrorCode(() -> service.search(null, null, null, null, null, null, -1, 20), ErrorCode.INVALID_INPUT);
        assertErrorCode(() -> service.search(null, null, null, null, null, null, 0, 0), ErrorCode.INVALID_INPUT);
        assertErrorCode(() -> service.search(null, null, null, null, null, null, 0, 51), ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("응답: 상담번호 형식, AI 단독이면 handledBy=AI·상담원 null, 페이지 정보와 hasNext")
    void response() {
        LocalDateTime started = LocalDateTime.of(2026, 11, 10, 16, 45);
        when(repository.search(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        new AdminConsultationRow(13L, started, 10L, "user06", null, null, "CLOSED", 0L),
                        new AdminConsultationRow(1L, started, 8L, "user04", 4L, "장건웅", "TERMINATED", 2L)
                ), PageRequest.of(0, 2), 5));

        PageResponse<AdminConsultationResponseDTO> result = service.search(null, null, null, null, null, null, 0, 2);

        AdminConsultationResponseDTO aiOnly = result.content().get(0);
        assertThat(aiOnly.consultationNo()).isEqualTo("C-000013");
        assertThat(aiOnly.handledBy()).isEqualTo("AI");
        assertThat(aiOnly.agentId()).isNull();
        assertThat(result.content().get(1).handledBy()).isEqualTo("AGENT");
        assertThat(result.content().get(1).abuseCount()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(5);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.hasNext()).isTrue();
    }

    private static void assertErrorCode(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
