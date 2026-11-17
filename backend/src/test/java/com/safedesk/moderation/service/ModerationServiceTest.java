package com.safedesk.moderation.service;

import com.safedesk.chat.entity.Consultation;
import com.safedesk.chat.entity.ConsultationStatus;
import com.safedesk.moderation.dto.AbuseFilterResult;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
        moderationService = new ModerationService(
            abuseFilter,
            abuseLogRepository,
            warningRepository,
            sanctionPolicyRepository,
            consultationRepository
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
}
