package com.safedesk.history.customer;

import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.common.response.PageResponse;
import com.safedesk.history.customer.dto.CustomerConsultationResponseDTO;
import com.safedesk.history.customer.dto.CustomerSanctionResponseDTO;
import com.safedesk.sanction.entity.SanctionPolicy;
import com.safedesk.sanction.repository.SanctionPolicyRepository;
import com.safedesk.sanction.repository.ViolationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * HIS-01 내 상담 이력, HIS-02 내 이용 상태. 조건 변환·응답 가공·정지 상태 계산을 본다.
 * (쿼리는 실제 DB 로 따로 확인)
 */
class CustomerHistoryServiceTest {

    private static final long CUSTOMER_ID = 8L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 18, 14, 0);

    private CustomerHistoryRepository repository;
    private ViolationRepository violationRepository;
    private SanctionPolicyRepository sanctionPolicyRepository;
    private CustomerHistoryService service;

    @BeforeEach
    void setUp() {
        repository = mock(CustomerHistoryRepository.class);
        violationRepository = mock(ViolationRepository.class);
        sanctionPolicyRepository = mock(SanctionPolicyRepository.class);
        service = new CustomerHistoryService(repository, violationRepository, sanctionPolicyRepository);

        when(repository.searchMyConsultations(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        SanctionPolicy policy = mock(SanctionPolicy.class);
        when(policy.getFirstDays()).thenReturn(7);
        when(policy.getSecondDays()).thenReturn(30);
        when(policy.getThirdDays()).thenReturn(90);
        when(sanctionPolicyRepository.findFirstByOrderByPolicyIdDesc()).thenReturn(Optional.of(policy));
    }

    // ---------- HIS-01 ----------

    @Test
    @DisplayName("HIS-01 type: ALL·빈 값은 조건 없음(null), AI 는 false, AGENT 는 true")
    void typeFilter() {
        service.getMyConsultations(CUSTOMER_ID, null, 0, 20);
        verify(repository).searchMyConsultations(eq(CUSTOMER_ID), isNull(), eq(PageRequest.of(0, 20)));

        service.getMyConsultations(CUSTOMER_ID, "AI", 0, 2);
        verify(repository).searchMyConsultations(eq(CUSTOMER_ID), eq(false), eq(PageRequest.of(0, 2)));

        service.getMyConsultations(CUSTOMER_ID, "AGENT", 0, 20);
        verify(repository).searchMyConsultations(eq(CUSTOMER_ID), eq(true), any());
    }

    @Test
    @DisplayName("HIS-01 잘못된 type·page·size 는 400 INVALID_INPUT")
    void invalidInput() {
        assertErrorCode(() -> service.getMyConsultations(CUSTOMER_ID, "BOT", 0, 20), ErrorCode.INVALID_INPUT);
        assertErrorCode(() -> service.getMyConsultations(CUSTOMER_ID, null, -1, 20), ErrorCode.INVALID_INPUT);
        assertErrorCode(() -> service.getMyConsultations(CUSTOMER_ID, null, 0, 0), ErrorCode.INVALID_INPUT);
        assertErrorCode(() -> service.getMyConsultations(CUSTOMER_ID, null, 0, 51), ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("HIS-01 응답: 상담번호, AI/AGENT, 요약 묶음, 위반 기록 여부")
    void consultationResponse() {
        LocalDateTime started = LocalDateTime.of(2026, 11, 3, 17, 20);
        when(repository.searchMyConsultations(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        new CustomerConsultationRow(6L, started, started.plusMinutes(8), 3L, "TERMINATED",
                                "불법 주정차 신고", "기타", "AI 응대 → … → 반복 폭언으로 자동 종료", "TERMINATED", 1L),
                        new CustomerConsultationRow(13L, started, started.plusMinutes(1), null, "CLOSED",
                                "분실물 문의", "대중교통", "AI 응대 → 고객 종료", "RESOLVED", 0L)
                ), PageRequest.of(0, 2), 3));

        PageResponse<CustomerConsultationResponseDTO> result = service.getMyConsultations(CUSTOMER_ID, null, 0, 2);

        CustomerConsultationResponseDTO terminated = result.content().get(0);
        assertThat(terminated.consultationNo()).isEqualTo("C-000006");
        assertThat(terminated.handledBy()).isEqualTo("AGENT");
        assertThat(terminated.summary().result()).isEqualTo("TERMINATED");
        assertThat(terminated.violationRecorded()).isTrue();

        CustomerConsultationResponseDTO aiOnly = result.content().get(1);
        assertThat(aiOnly.handledBy()).isEqualTo("AI");
        assertThat(aiOnly.violationRecorded()).isFalse();
        assertThat(result.hasNext()).isTrue();
    }

    // ---------- HIS-02 ----------

    @Test
    @DisplayName("HIS-02 정지 중이 아니면 NONE, 상담 시작 가능, 현재 제재 null, 누적 위반은 그대로")
    void notSuspended() {
        when(repository.findActiveSanction(CUSTOMER_ID, NOW)).thenReturn(Optional.empty());
        when(violationRepository.countByCustomerId(CUSTOMER_ID)).thenReturn(1L);

        CustomerSanctionResponseDTO result = service.getMySanction(CUSTOMER_ID, NOW);

        assertThat(result.sanctionStatus()).isEqualTo("NONE");
        assertThat(result.canStartConsultation()).isTrue();
        assertThat(result.currentSanction()).isNull();
        assertThat(result.totalViolationCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("HIS-02 정지 중이면 SUSPENDED, 남은 일수는 올림, 자동 종료 사유 문구")
    void suspended() {
        LocalDateTime endAt = NOW.plusDays(17).plusHours(2);   // 17일 2시간 남음 → D-18
        when(repository.findActiveSanction(CUSTOMER_ID, NOW))
                .thenReturn(Optional.of(sanction(7L, 30, NOW.minusDays(13), endAt, "ABUSE_AUTO")));
        when(violationRepository.countByCustomerId(CUSTOMER_ID)).thenReturn(2L);

        CustomerSanctionResponseDTO result = service.getMySanction(CUSTOMER_ID, NOW);

        assertThat(result.sanctionStatus()).isEqualTo("SUSPENDED");
        assertThat(result.canStartConsultation()).isFalse();
        assertThat(result.totalViolationCount()).isEqualTo(2);
        assertThat(result.currentSanction().suspendDays()).isEqualTo(30);
        assertThat(result.currentSanction().remainingDays()).isEqualTo(18);
        assertThat(result.currentSanction().reason()).isEqualTo("상담 중 반복 폭언 (자동 종료)");
    }

    @Test
    @DisplayName("HIS-02 상담원 강제 종료 사유, 해제가 1초만 남아도 D-1")
    void manualReasonAndRoundUp() {
        when(repository.findActiveSanction(CUSTOMER_ID, NOW))
                .thenReturn(Optional.of(sanction(9L, 7, NOW.minusDays(7), NOW.plusSeconds(1), "ABUSE_MANUAL")));

        CustomerSanctionResponseDTO.CurrentSanction current = service.getMySanction(CUSTOMER_ID, NOW).currentSanction();

        assertThat(current.remainingDays()).isEqualTo(1);
        assertThat(current.reason()).isEqualTo("상담 중 폭언 (상담원 종료)");
    }

    @Test
    @DisplayName("HIS-02 제재 기준 안내는 최신 정책의 1회·2회·3회 이상 정지 일수")
    void policyGuide() {
        when(repository.findActiveSanction(any(), any())).thenReturn(Optional.empty());

        var guide = service.getMySanction(CUSTOMER_ID, NOW).policyGuide();

        assertThat(guide).containsExactly(
                new CustomerSanctionResponseDTO.PolicyGuide(1, 7, false),
                new CustomerSanctionResponseDTO.PolicyGuide(2, 30, false),
                new CustomerSanctionResponseDTO.PolicyGuide(3, 90, true));
    }

    // ---- 준비용 도우미 ----

    private static ActiveSanctionInfo sanction(long id, int days, LocalDateTime startAt, LocalDateTime endAt,
                                               String closeReason) {
        return new ActiveSanctionInfo() {
            public Long getSanctionId() { return id; }
            public Number getSuspendDays() { return days; }
            public LocalDateTime getStartAt() { return startAt; }
            public LocalDateTime getEndAt() { return endAt; }
            public String getCloseReason() { return closeReason; }
        };
    }

    private static void assertErrorCode(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
