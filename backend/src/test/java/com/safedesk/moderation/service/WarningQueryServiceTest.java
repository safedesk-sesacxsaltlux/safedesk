package com.safedesk.moderation.service;

import com.safedesk.chat.entity.Consultation;
import com.safedesk.chat.repository.ConsultationRepository;
import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.moderation.dto.WarningHistoryResponseDTO;
import com.safedesk.moderation.entity.Warning;
import com.safedesk.moderation.entity.WarningType;
import com.safedesk.moderation.repository.WarningRepository;
import com.safedesk.sanction.entity.SanctionPolicy;
import com.safedesk.sanction.repository.SanctionPolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MOD-05 상담별 경고 내역 조회 단위 테스트 (DB 없이, #74).
 *
 * Warning·Consultation·SanctionPolicy 는 직접 만들 수 없어서(생성자 없음, createdAt 은 DB 기본값) mock 으로 만든다.
 */
class WarningQueryServiceTest {

    private static final long CONSULTATION_ID = 101L;
    private static final long POLICY_ID = 1L;
    private static final long AGENT_ID = 2L;
    private static final long OTHER_AGENT_ID = 3L;
    private static final long ADMIN_ID = 1L;

    private WarningQueryService warningQueryService;
    private ConsultationRepository consultationRepository;
    private WarningRepository warningRepository;
    private SanctionPolicyRepository sanctionPolicyRepository;

    @BeforeEach
    void setUp() {
        consultationRepository = mock(ConsultationRepository.class);
        warningRepository = mock(WarningRepository.class);
        sanctionPolicyRepository = mock(SanctionPolicyRepository.class);
        warningQueryService = new WarningQueryService(
            consultationRepository, warningRepository, sanctionPolicyRepository);
    }

    // ───────── 도우미 (테스트마다 반복되는 mock 준비) ─────────

    /** agentId 가 담당하고 POLICY_ID 정책을 쓰는 상담을 consultationRepository 가 돌려주게 한다 */
    private void givenConsultation(Long agentId) {
        Consultation consultation = mock(Consultation.class);
        when(consultation.getAgentId()).thenReturn(agentId);
        when(consultation.getPolicyId()).thenReturn(POLICY_ID);
        when(consultationRepository.findById(CONSULTATION_ID)).thenReturn(Optional.of(consultation));
    }

    /** 종료 기준이 closeThreshold 인 정책을 돌려주게 한다 */
    private void givenPolicy(int closeThreshold) {
        SanctionPolicy policy = mock(SanctionPolicy.class);
        when(policy.getCloseThreshold()).thenReturn(closeThreshold);
        when(sanctionPolicyRepository.findByIdOrLatest(any())).thenReturn(policy);
    }

    /** 이 상담의 경고 목록을 돌려주게 한다 */
    private void givenWarnings(List<Warning> warnings) {
        when(warningRepository.findByConsultationIdOrderByCreatedAtAscWarningIdAsc(CONSULTATION_ID))
            .thenReturn(warnings);
    }

    /** 경고 한 건 mock */
    private Warning warning(long warningId, WarningType type) {
        Warning warning = mock(Warning.class);
        when(warning.getWarningId()).thenReturn(warningId);
        when(warning.getWarningType()).thenReturn(type);
        when(warning.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 11, 11, 13, 10));
        return warning;
    }

    // ───────── 정상 조회 ─────────

    @Test
    @DisplayName("담당 상담원은 경고 목록과 합계·자동 경고 수·종료 기준을 받는다")
    void agentGetsWarnings() {
        // Given: 자동 경고 1건 + 수동 경고 1건
        givenConsultation(AGENT_ID);
        givenPolicy(2);
        givenWarnings(List.of(warning(501L, WarningType.AUTO), warning(502L, WarningType.MANUAL)));

        // When
        WarningHistoryResponseDTO result = warningQueryService.getWarnings(CONSULTATION_ID, AGENT_ID, false);

        // Then: 합계는 AUTO + MANUAL, 자동 경고 수는 AUTO 만 (정책 4장 「수동 경고도 1회로 셈」)
        assertThat(result.getConsultationId()).isEqualTo(CONSULTATION_ID);
        assertThat(result.getWarningCount()).isEqualTo(2);
        assertThat(result.getAutoWarningCount()).isEqualTo(1);
        assertThat(result.getCloseThreshold()).isEqualTo(2);
        assertThat(result.getWarnings())
            .extracting(WarningHistoryResponseDTO.WarningItem::getWarningId)
            .containsExactly(501L, 502L);
        assertThat(result.getWarnings())
            .extracting(WarningHistoryResponseDTO.WarningItem::getWarningType)
            .containsExactly(WarningType.AUTO, WarningType.MANUAL);
    }

    @Test
    @DisplayName("종료 기준은 상담에 저장된 정책에서 가져온다")
    void closeThresholdFromConsultationPolicy() {
        // Given: 이 상담의 정책은 종료 기준 3회
        givenConsultation(AGENT_ID);
        givenPolicy(3);
        givenWarnings(List.of());

        // When
        WarningHistoryResponseDTO result = warningQueryService.getWarnings(CONSULTATION_ID, AGENT_ID, false);

        // Then: 「지금 활성 정책」이 아니라 상담의 policyId 로 조회했다 (MOD-02 자동 경고와 같은 기준)
        assertThat(result.getCloseThreshold()).isEqualTo(3);
        verify(sanctionPolicyRepository).findByIdOrLatest(POLICY_ID);
    }

    @Test
    @DisplayName("경고가 없으면 빈 목록과 0회")
    void noWarnings() {
        // Given
        givenConsultation(AGENT_ID);
        givenPolicy(2);
        givenWarnings(List.of());

        // When
        WarningHistoryResponseDTO result = warningQueryService.getWarnings(CONSULTATION_ID, AGENT_ID, false);

        // Then: null 이 아니라 빈 목록 (프론트가 바로 반복문을 돌릴 수 있다)
        assertThat(result.getWarningCount()).isZero();
        assertThat(result.getAutoWarningCount()).isZero();
        assertThat(result.getWarnings()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("관리자는 담당이 아니어도 조회한다")
    void adminCanViewAnyConsultation() {
        // Given: 담당은 AGENT_ID
        givenConsultation(AGENT_ID);
        givenPolicy(2);
        givenWarnings(List.of(warning(501L, WarningType.AUTO)));

        // When: 관리자가 조회
        WarningHistoryResponseDTO result = warningQueryService.getWarnings(CONSULTATION_ID, ADMIN_ID, true);

        // Then
        assertThat(result.getConsultationId()).isEqualTo(CONSULTATION_ID);
        assertThat(result.getWarningCount()).isEqualTo(1);
    }

    // ───────── 조회 차단 ─────────

    @Test
    @DisplayName("다른 상담원이 조회하면 CHAT_ACCESS_DENIED")
    void otherAgentDenied() {
        // Given: 담당은 AGENT_ID
        givenConsultation(AGENT_ID);

        // When + Then
        assertThatThrownBy(() -> warningQueryService.getWarnings(CONSULTATION_ID, OTHER_AGENT_ID, false))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.CHAT_ACCESS_DENIED);

        // 막힌 뒤에는 경고 목록을 조회하지 않는다
        verify(warningRepository, never()).findByConsultationIdOrderByCreatedAtAscWarningIdAsc(any());
    }

    @Test
    @DisplayName("수락 전(담당 상담원 없음) 상담을 상담원이 조회하면 CHAT_ACCESS_DENIED")
    void unassignedConsultationDenied() {
        // Given: 아직 아무도 수락하지 않아 agentId 가 null
        givenConsultation(null);

        // When + Then: NullPointerException 이 아니라 403
        assertThatThrownBy(() -> warningQueryService.getWarnings(CONSULTATION_ID, AGENT_ID, false))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.CHAT_ACCESS_DENIED);
    }

    @Test
    @DisplayName("상담이 없으면 CHAT_NOT_FOUND")
    void consultationNotFound() {
        // Given
        when(consultationRepository.findById(CONSULTATION_ID)).thenReturn(Optional.empty());

        // When + Then
        assertThatThrownBy(() -> warningQueryService.getWarnings(CONSULTATION_ID, AGENT_ID, false))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.CHAT_NOT_FOUND);
    }
}
