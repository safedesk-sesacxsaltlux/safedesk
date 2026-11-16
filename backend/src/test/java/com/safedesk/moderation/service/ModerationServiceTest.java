package com.safedesk.moderation.service;

import com.safedesk.moderation.dto.AbuseFilterResult;
import com.safedesk.moderation.entity.AbuseLog;
import com.safedesk.moderation.entity.AbuseStatus;
import com.safedesk.moderation.filter.AbuseFilter;
import com.safedesk.moderation.filter.BannedWordCache;
import com.safedesk.moderation.filter.TextNormalizer;
import com.safedesk.moderation.repository.AbuseLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DB 없이 도는 단위 테스트 (이슈 #25 완료 조건).
 */
class ModerationServiceTest {

    private ModerationService moderationService;
    private AbuseLogRepository abuseLogRepository;

    @BeforeEach
    void setUp() {
        // 판정 로직(AbuseFilter)은 진짜로 돌리고, DB 에 닿는 금칙어 캐시·저장소만 mock 으로 바꾼다
        TextNormalizer textNormalizer = new TextNormalizer();
        BannedWordCache bannedWordCache = mock(BannedWordCache.class);
        when(bannedWordCache.getWords()).thenReturn(List.of("시발", "병신", "새끼"));
        AbuseFilter abuseFilter = new AbuseFilter(bannedWordCache, textNormalizer);
        abuseLogRepository = mock(AbuseLogRepository.class);
        moderationService = new ModerationService(abuseFilter, abuseLogRepository);
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
}
