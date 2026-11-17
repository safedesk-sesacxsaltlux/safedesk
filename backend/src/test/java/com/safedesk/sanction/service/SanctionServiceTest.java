package com.safedesk.sanction.service;

import com.safedesk.sanction.dto.SanctionResult;
import com.safedesk.sanction.entity.Sanction;
import com.safedesk.sanction.entity.SanctionPolicy;
import com.safedesk.sanction.entity.Violation;
import com.safedesk.sanction.repository.SanctionPolicyRepository;
import com.safedesk.sanction.repository.SanctionRepository;
import com.safedesk.sanction.repository.ViolationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DB 없이 도는 단위 테스트 (MOD-04 이슈 #54 완료 조건).
 * ModerationServiceTest 처럼 저장소는 mock 으로 바꾸고, SanctionPolicy 는 mock 으로 정지 일수를 돌려준다.
 */
class SanctionServiceTest {

    private static final long CONSULTATION_ID = 100L;
    private static final long CUSTOMER_ID = 1L;
    private static final Long POLICY_ID = 5L;
    private static final LocalDateTime CLOSED_AT = LocalDateTime.of(2026, 11, 17, 14, 30);

    private SanctionService sanctionService;
    private ViolationRepository violationRepository;
    private SanctionRepository sanctionRepository;
    private SanctionPolicyRepository sanctionPolicyRepository;

    @BeforeEach
    void setUp() {
        violationRepository = mock(ViolationRepository.class);
        sanctionRepository = mock(SanctionRepository.class);
        sanctionPolicyRepository = mock(SanctionPolicyRepository.class);

        // 상담 정책(7/30/90)과 최신 정책(14/60/180)의 값을 다르게 둬서, 정지 일수만 보고도 어느 정책을 썼는지 알 수 있게 한다
        SanctionPolicy consultationPolicy = policy(7, 30, 90);
        SanctionPolicy latestPolicy = policy(14, 60, 180);
        // 정책 조회는 default 메서드라 mock 이면 null 을 돌려준다. 실제 로직(findById → 최신 정책)을 타게 한다
        when(sanctionPolicyRepository.findByIdOrLatest(any())).thenCallRealMethod();
        when(sanctionPolicyRepository.findById(POLICY_ID)).thenReturn(Optional.of(consultationPolicy));
        when(sanctionPolicyRepository.findFirstByOrderByPolicyIdDesc()).thenReturn(Optional.of(latestPolicy));

        when(violationRepository.findByConsultationId(CONSULTATION_ID)).thenReturn(Optional.empty());
        // save() 는 받은 엔티티를 그대로 돌려준다 (DB 가 없으므로 ID 는 null)
        when(violationRepository.save(any(Violation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sanctionRepository.save(any(Sanction.class))).thenAnswer(inv -> inv.getArgument(0));

        sanctionService = new SanctionService(violationRepository, sanctionRepository, sanctionPolicyRepository);
    }

    @Test
    @DisplayName("첫 위반이면 위반 1건, 7일 정지를 저장하고 해제 일시는 종료 시각 + 7일이다")
    void firstViolation_suspends7Days() {
        when(violationRepository.countByCustomerId(CUSTOMER_ID)).thenReturn(1L);

        SanctionResult result = sanctionService.recordViolationAndSuspend(
            CONSULTATION_ID, CUSTOMER_ID, POLICY_ID, CLOSED_AT);

        ArgumentCaptor<Violation> violationCaptor = ArgumentCaptor.forClass(Violation.class);
        verify(violationRepository).save(violationCaptor.capture());
        assertThat(violationCaptor.getValue().getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(violationCaptor.getValue().getConsultationId()).isEqualTo(CONSULTATION_ID);

        ArgumentCaptor<Sanction> sanctionCaptor = ArgumentCaptor.forClass(Sanction.class);
        verify(sanctionRepository).save(sanctionCaptor.capture());
        Sanction saved = sanctionCaptor.getValue();
        assertThat(saved.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(saved.getSuspendDays()).isEqualTo(7);
        assertThat(saved.getStartAt()).isEqualTo(CLOSED_AT);
        assertThat(saved.getEndAt()).isEqualTo(CLOSED_AT.plusDays(7));

        assertThat(result.totalViolations()).isEqualTo(1L);
        assertThat(result.suspendDays()).isEqualTo(7);
        assertThat(result.endAt()).isEqualTo(CLOSED_AT.plusDays(7));
    }

    @ParameterizedTest(name = "누적 {0}회 → {1}일")
    @CsvSource({"2, 30", "3, 90", "4, 90"})
    @DisplayName("누적 2회면 30일, 3회 이상이면 90일 정지한다")
    void suspendDaysByTotalViolations(long totalViolations, int expectedDays) {
        when(violationRepository.countByCustomerId(CUSTOMER_ID)).thenReturn(totalViolations);

        SanctionResult result = sanctionService.recordViolationAndSuspend(
            CONSULTATION_ID, CUSTOMER_ID, POLICY_ID, CLOSED_AT);

        assertThat(result.totalViolations()).isEqualTo(totalViolations);
        assertThat(result.suspendDays()).isEqualTo(expectedDays);
        assertThat(result.endAt()).isEqualTo(CLOSED_AT.plusDays(expectedDays));
    }

    @Test
    @DisplayName("상담의 policy_id 정책값으로 정지 일수를 정한다")
    void usesConsultationPolicy() {
        when(violationRepository.countByCustomerId(CUSTOMER_ID)).thenReturn(1L);

        SanctionResult result = sanctionService.recordViolationAndSuspend(
            CONSULTATION_ID, CUSTOMER_ID, POLICY_ID, CLOSED_AT);

        assertThat(result.suspendDays()).isEqualTo(7);
        verify(sanctionPolicyRepository).findById(POLICY_ID);
        verify(sanctionPolicyRepository, never()).findFirstByOrderByPolicyIdDesc();
    }

    @Test
    @DisplayName("policy_id 가 없으면 최신 정책을 쓴다")
    void fallsBackToLatestPolicy() {
        when(violationRepository.countByCustomerId(CUSTOMER_ID)).thenReturn(1L);

        SanctionResult result = sanctionService.recordViolationAndSuspend(
            CONSULTATION_ID, CUSTOMER_ID, null, CLOSED_AT);

        assertThat(result.suspendDays()).isEqualTo(14);
        verify(sanctionPolicyRepository).findFirstByOrderByPolicyIdDesc();
        verify(sanctionPolicyRepository, never()).findById(any());
    }

    @Test
    @DisplayName("같은 상담의 위반이 이미 있으면 새로 저장하지 않고 기존 결과를 돌려준다")
    void existingViolation_returnsExistingResult() {
        // setter 가 없어 ID 를 채울 수 없으므로 엔티티를 mock 으로 만든다
        Violation existingViolation = mock(Violation.class);
        when(existingViolation.getViolationId()).thenReturn(10L);
        when(existingViolation.getCustomerId()).thenReturn(CUSTOMER_ID);

        Sanction existingSanction = mock(Sanction.class);
        when(existingSanction.getSanctionId()).thenReturn(20L);
        when(existingSanction.getSuspendDays()).thenReturn(7);
        when(existingSanction.getStartAt()).thenReturn(CLOSED_AT);
        when(existingSanction.getEndAt()).thenReturn(CLOSED_AT.plusDays(7));

        when(violationRepository.findByConsultationId(CONSULTATION_ID)).thenReturn(Optional.of(existingViolation));
        when(sanctionRepository.findByViolationId(10L)).thenReturn(Optional.of(existingSanction));
        when(violationRepository.countByCustomerId(CUSTOMER_ID)).thenReturn(1L);

        SanctionResult result = sanctionService.recordViolationAndSuspend(
            CONSULTATION_ID, CUSTOMER_ID, POLICY_ID, CLOSED_AT);

        verify(violationRepository, never()).save(any());
        verify(sanctionRepository, never()).save(any());
        assertThat(result.violationId()).isEqualTo(10L);
        assertThat(result.sanctionId()).isEqualTo(20L);
        assertThat(result.suspendDays()).isEqualTo(7);
        assertThat(result.endAt()).isEqualTo(CLOSED_AT.plusDays(7));
    }

    @Test
    @DisplayName("저장이 실패하면 예외가 그대로 올라간다 (호출한 종료 트랜잭션이 롤백되도록)")
    void saveFailure_throws() {
        // 여기서 예외를 삼키면 종료는 됐는데 정지가 없는 상태가 된다
        when(violationRepository.countByCustomerId(CUSTOMER_ID)).thenReturn(1L);
        RuntimeException dbError = new RuntimeException("DB 오류");
        doThrow(dbError).when(sanctionRepository).save(any(Sanction.class));

        assertThatThrownBy(() -> sanctionService.recordViolationAndSuspend(
            CONSULTATION_ID, CUSTOMER_ID, POLICY_ID, CLOSED_AT))
            .isSameAs(dbError);
    }

    private static SanctionPolicy policy(int firstDays, int secondDays, int thirdDays) {
        SanctionPolicy policy = mock(SanctionPolicy.class);
        when(policy.getFirstDays()).thenReturn(firstDays);
        when(policy.getSecondDays()).thenReturn(secondDays);
        when(policy.getThirdDays()).thenReturn(thirdDays);
        return policy;
    }
}
