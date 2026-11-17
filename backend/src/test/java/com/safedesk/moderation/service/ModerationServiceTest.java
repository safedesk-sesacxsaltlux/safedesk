package com.safedesk.moderation.service;

import com.safedesk.chat.entity.Consultation;
import com.safedesk.chat.entity.CloseReason;
import com.safedesk.chat.entity.ClosedBy;
import com.safedesk.chat.entity.ConsultationStatus;
import com.safedesk.moderation.dto.AbuseFilterResult;
import com.safedesk.moderation.dto.TerminationResult;
import com.safedesk.common.event.ConsultationClosedEvent;
import com.safedesk.sanction.dto.SanctionResult;
import com.safedesk.moderation.dto.WarningResult;
import com.safedesk.moderation.entity.AbuseLog;
import com.safedesk.moderation.entity.AbuseStatus;
import com.safedesk.moderation.entity.Warning;
import com.safedesk.moderation.entity.WarningType;
import com.safedesk.sanction.entity.SanctionPolicy;
import com.safedesk.moderation.filter.AbuseFilter;
import com.safedesk.moderation.filter.BannedWordCache;
import com.safedesk.moderation.filter.TextNormalizer;
import com.safedesk.moderation.repository.AbuseLogRepository;
import com.safedesk.moderation.repository.WarningRepository;
import com.safedesk.sanction.repository.SanctionPolicyRepository;
import com.safedesk.chat.repository.ConsultationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import com.safedesk.sanction.service.SanctionService;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DB 없이 도는 단위 테스트 (MOD-01 이슈 #25, MOD-02 완료 조건).
 */
class ModerationServiceTest {

    private ModerationService moderationService;
    private AbuseLogRepository abuseLogRepository;
    private WarningRepository warningRepository;
    private SanctionPolicyRepository sanctionPolicyRepository;
    private ConsultationRepository consultationRepository;
    private SanctionService sanctionService;
    private ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        // 판정 로직(AbuseFilter)은 진짜로 돌리고, DB 에 닿는 금칙어 캐시·저장소만 mock 으로 바꾼다
        TextNormalizer textNormalizer = new TextNormalizer();
        BannedWordCache bannedWordCache = mock(BannedWordCache.class);
        when(bannedWordCache.getWords()).thenReturn(List.of("시발", "병신", "새끼"));
        AbuseFilter abuseFilter = new AbuseFilter(bannedWordCache, textNormalizer);
        abuseLogRepository = mock(AbuseLogRepository.class);
        warningRepository = mock(WarningRepository.class);
        sanctionPolicyRepository = mock(SanctionPolicyRepository.class);
        // 정책 조회는 default 메서드라 mock 이면 null 을 돌려준다. 실제 로직(findById → 최신 정책)을 타게 한다
        when(sanctionPolicyRepository.findByIdOrLatest(any())).thenCallRealMethod();
        consultationRepository = mock(ConsultationRepository.class);
        sanctionService = mock(SanctionService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        moderationService = new ModerationService(
            abuseFilter,
            abuseLogRepository,
            warningRepository,
            sanctionPolicyRepository,
            consultationRepository,
            sanctionService,
            eventPublisher
        );
    }

    @Test
    @DisplayName("금칙어가 있는 메시지는 ABUSIVE, 금칙어만 *** 로 가린다")
    void abusiveMessage() {
        AbuseFilterResult result = moderationService.processMessage("상담원 진짜 병신같네");

        assertThat(result.abuseStatus()).isEqualTo(AbuseStatus.ABUSIVE);
        assertThat(result.maskedContent()).isEqualTo("상담원 진짜 ***같네");
    }

    @Test
    @DisplayName("정상 메시지는 NORMAL, 원문 그대로")
    void normalMessage() {
        AbuseFilterResult result = moderationService.processMessage("환불 처리가 언제 되나요?");

        assertThat(result.abuseStatus()).isEqualTo(AbuseStatus.NORMAL);
        assertThat(result.maskedContent()).isEqualTo("환불 처리가 언제 되나요?");
    }

    @Test
    @DisplayName("원문은 messageId·consultationId 와 함께 abuse_log 에 저장된다")
    void saveOriginal() {
        // mock 저장소가 진짜처럼 받은 객체를 그대로 돌려주게 한다
        when(abuseLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        moderationService.saveOriginal(100L, 1L, "원문");

        ArgumentCaptor<AbuseLog> captor = ArgumentCaptor.forClass(AbuseLog.class);
        verify(abuseLogRepository).save(captor.capture());   // save()가 호출됐는지 + 넘어간 값 붙잡기
        AbuseLog saved = captor.getValue();                   // 붙잡은 값 꺼내기

        assertThat(saved.getMessageId()).isEqualTo(100L);
        assertThat(saved.getConsultationId()).isEqualTo(1L);
        assertThat(saved.getOriginalContent()).isEqualTo("원문");
    }

    // ===== 자동 경고 (MOD-02) =====

    @Test
    @DisplayName("첫 폭언이면 AUTO 경고를 저장하고, 아직 종료 대상이 아니다")
    void firstWarning() {
        givenConsultation(ConsultationStatus.IN_PROGRESS, 1L);
        givenPolicy(1L, 2);
        when(warningRepository.countByConsultationId(1L)).thenReturn(1L);

        Optional<WarningResult> result = moderationService.applyAutoWarning(1L, 10L);

        // 저장된 경고: 이 상담·이 폭언 기록, AUTO, 경고한 상담원 없음
        ArgumentCaptor<Warning> captor = ArgumentCaptor.forClass(Warning.class);
        verify(warningRepository).save(captor.capture());
        Warning saved = captor.getValue();
        assertThat(saved.getConsultationId()).isEqualTo(1L);
        assertThat(saved.getAbuseLogId()).isEqualTo(10L);
        assertThat(saved.getWarningType()).isEqualTo(WarningType.AUTO);
        assertThat(saved.getIssuedBy()).isNull();

        assertThat(result).isPresent();
        assertThat(result.get().warningCount()).isEqualTo(1);
        assertThat(result.get().closeThreshold()).isEqualTo(2);
        assertThat(result.get().shouldTerminate()).isFalse();
    }

    @Test
    @DisplayName("경고 횟수가 종료 기준에 도달하면 종료 대상이다")
    void reachCloseThreshold() {
        givenConsultation(ConsultationStatus.IN_PROGRESS, 1L);
        givenPolicy(1L, 2);
        when(warningRepository.countByConsultationId(1L)).thenReturn(2L);

        Optional<WarningResult> result = moderationService.applyAutoWarning(1L, 10L);

        assertThat(result).isPresent();
        assertThat(result.get().warningCount()).isEqualTo(2);
        assertThat(result.get().shouldTerminate()).isTrue();
    }

    @Test
    @DisplayName("같은 폭언 기록으로는 경고를 다시 만들지 않는다")
    void duplicateWarning() {
        givenConsultation(ConsultationStatus.IN_PROGRESS, 1L);
        givenPolicy(1L, 2);
        when(warningRepository.existsByAbuseLogId(10L)).thenReturn(true);

        Optional<WarningResult> result = moderationService.applyAutoWarning(1L, 10L);

        assertThat(result).isEmpty();
        verify(warningRepository, never()).save(any());
    }

    @Test
    @DisplayName("종료된 상담에는 경고를 만들지 않는다")
    void closedConsultation() {
        givenConsultation(ConsultationStatus.TERMINATED, 1L);
        givenPolicy(1L, 2);

        Optional<WarningResult> result = moderationService.applyAutoWarning(1L, 10L);

        assertThat(result).isEmpty();
        verify(warningRepository, never()).save(any());
    }

    @Test
    @DisplayName("상담에 저장된 정책 기준으로 판단한다 (정책을 바꿔도 새 상담부터 적용, S3)")
    void useConsultationPolicy() {
        // 이 상담은 종료 기준 3회 정책(2번)으로 시작했다. 최신 정책은 2회라고 해도 3회로 판단해야 한다
        givenConsultation(ConsultationStatus.IN_PROGRESS, 2L);
        givenPolicy(2L, 3);
        SanctionPolicy latest = mock(SanctionPolicy.class);
        when(latest.getCloseThreshold()).thenReturn(2);
        when(sanctionPolicyRepository.findFirstByOrderByPolicyIdDesc()).thenReturn(Optional.of(latest));
        when(warningRepository.countByConsultationId(1L)).thenReturn(2L);

        Optional<WarningResult> result = moderationService.applyAutoWarning(1L, 10L);

        assertThat(result).isPresent();
        assertThat(result.get().closeThreshold()).isEqualTo(3);
        assertThat(result.get().shouldTerminate()).isFalse();
        verify(sanctionPolicyRepository, never()).findFirstByOrderByPolicyIdDesc();
    }

    @Test
    @DisplayName("상담에 정책 ID 가 없으면 최신 정책으로 판단한다")
    void fallbackToLatestPolicy() {
        givenConsultation(ConsultationStatus.IN_PROGRESS, null);
        SanctionPolicy latest = mock(SanctionPolicy.class);
        when(latest.getCloseThreshold()).thenReturn(2);
        when(sanctionPolicyRepository.findFirstByOrderByPolicyIdDesc()).thenReturn(Optional.of(latest));
        when(warningRepository.countByConsultationId(1L)).thenReturn(1L);

        Optional<WarningResult> result = moderationService.applyAutoWarning(1L, 10L);

        assertThat(result).isPresent();
        assertThat(result.get().closeThreshold()).isEqualTo(2);
    }

    // 상담 1번: 주어진 상태와 정책 ID. 경고 저장은 받은 객체를 그대로 돌려준다
    private void givenConsultation(String status, Long policyId) {
        Consultation consultation = mock(Consultation.class);
        when(consultation.getStatus()).thenReturn(status);
        when(consultation.getPolicyId()).thenReturn(policyId);
        when(consultationRepository.findById(1L)).thenReturn(Optional.of(consultation));
        when(warningRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void givenPolicy(Long policyId, int closeThreshold) {
        SanctionPolicy policy = mock(SanctionPolicy.class);
        when(policy.getCloseThreshold()).thenReturn(closeThreshold);
        when(sanctionPolicyRepository.findById(policyId)).thenReturn(Optional.of(policy));
    }

    // ===== MOD-03 폭언 누적 자동 종료 =====

    private static final LocalDateTime CLOSED_AT = LocalDateTime.of(2026, 11, 17, 14, 30);

    @Test
    @DisplayName("종료에 성공하면 위반·정지를 만들고 요약 이벤트를 1번 발행한다")
    void terminateForAbuse_success() {
        givenTerminatableConsultation(100L, 5L);
        when(consultationRepository.terminateForAbuse(eq(1L), any())).thenReturn(1);
        SanctionResult sanction = new SanctionResult(
            201L, 100L, 1L, 301L, 7, CLOSED_AT, CLOSED_AT.plusDays(7));
        when(sanctionService.recordViolationAndSuspend(anyLong(), anyLong(), any(), any())).thenReturn(sanction);

        Optional<TerminationResult> result = moderationService.terminateForAbuse(1L);

        // UPDATE 에 넘긴 종료 시각과 MOD-04 에 넘긴 정지 시작 시각이 같은 값이다 (REQ-021)
        ArgumentCaptor<LocalDateTime> updateClosedAt = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(consultationRepository).terminateForAbuse(eq(1L), updateClosedAt.capture());
        ArgumentCaptor<LocalDateTime> sanctionStartAt = ArgumentCaptor.forClass(LocalDateTime.class);
        // 상담의 고객 ID·정책 ID 를 넘긴다
        verify(sanctionService).recordViolationAndSuspend(eq(1L), eq(100L), eq(5L), sanctionStartAt.capture());
        assertThat(sanctionStartAt.getValue()).isEqualTo(updateClosedAt.getValue());

        verify(eventPublisher, times(1)).publishEvent(any(ConsultationClosedEvent.class));

        assertThat(result).isPresent();
        TerminationResult terminated = result.get();
        assertThat(terminated.consultationId()).isEqualTo(1L);
        assertThat(terminated.status()).isEqualTo(ConsultationStatus.TERMINATED);
        assertThat(terminated.closeReason()).isEqualTo(CloseReason.ABUSE_AUTO);
        assertThat(terminated.closedBy()).isEqualTo(ClosedBy.SYSTEM);
        assertThat(terminated.closedAt()).isEqualTo(updateClosedAt.getValue());
        assertThat(terminated.suspendDays()).isEqualTo(7);
        assertThat(terminated.suspendEndAt()).isEqualTo(CLOSED_AT.plusDays(7));
    }

    @Test
    @DisplayName("이미 종료된 상담이면 empty, 위반·이벤트를 만들지 않는다")
    void terminateForAbuse_alreadyClosed() {
        givenTerminatableConsultation(100L, 5L);
        // 0행: 이미 종료됐거나 상담원이 먼저 종료한 경우
        when(consultationRepository.terminateForAbuse(eq(1L), any())).thenReturn(0);

        Optional<TerminationResult> result = moderationService.terminateForAbuse(1L);

        assertThat(result).isEmpty();
        verify(sanctionService, never()).recordViolationAndSuspend(anyLong(), anyLong(), any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("위반·정지 저장이 실패하면 예외가 그대로 올라가고 이벤트를 발행하지 않는다")
    void terminateForAbuse_sanctionFailure_throws() {
        // 여기서 예외를 삼키면 종료는 됐는데 정지가 없는 상태가 된다 (NFR-009)
        givenTerminatableConsultation(100L, 5L);
        when(consultationRepository.terminateForAbuse(eq(1L), any())).thenReturn(1);
        RuntimeException dbError = new RuntimeException("DB 오류");
        when(sanctionService.recordViolationAndSuspend(anyLong(), anyLong(), any(), any())).thenThrow(dbError);

        assertThatThrownBy(() -> moderationService.terminateForAbuse(1L)).isSameAs(dbError);
        verify(eventPublisher, never()).publishEvent(any());
    }

    // 상담 1번: 상담 중, 주어진 고객 ID·정책 ID
    private void givenTerminatableConsultation(long customerId, Long policyId) {
        Consultation consultation = mock(Consultation.class);
        when(consultation.getStatus()).thenReturn(ConsultationStatus.IN_PROGRESS);
        when(consultation.getCustomerId()).thenReturn(customerId);
        when(consultation.getPolicyId()).thenReturn(policyId);
        when(consultationRepository.findById(1L)).thenReturn(Optional.of(consultation));
    }
}
