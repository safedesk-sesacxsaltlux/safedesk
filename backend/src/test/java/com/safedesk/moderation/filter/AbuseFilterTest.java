package com.safedesk.moderation.filter;

import com.safedesk.moderation.dto.AbuseFilterResult;
import com.safedesk.moderation.entity.AbuseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * DB 없이 도는 단위 테스트. BannedWordCache 는 Mockito mock 으로 대체한다.
 * 이슈 #10 완료 조건과 1:1 대응.
 */
class AbuseFilterTest {

    private AbuseFilter abuseFilter;

    @BeforeEach
    void setUp() {
        TextNormalizer textNormalizer = new TextNormalizer();

        // 실제 BannedWordCache.load() 처럼 정규화된 금칙어를 돌려준다
        BannedWordCache bannedWordCache = mock(BannedWordCache.class);
        when(bannedWordCache.getWords()).thenReturn(List.of("시발", "ㅆ발", "병신", "미친", "새끼", "개새끼"));

        abuseFilter = new AbuseFilter(bannedWordCache, textNormalizer);
    }

    @Test
    @DisplayName("금칙어가 포함된 메시지는 ABUSIVE, 금칙어만 ***로 치환")
    void abusiveMessageIsMasked() {
        AbuseFilterResult result = abuseFilter.filter("상담원 진짜 병신같네");

        assertThat(result.abuseStatus()).isEqualTo(AbuseStatus.ABUSIVE);
        assertThat(result.detected()).isTrue();
        assertThat(result.maskedContent()).isEqualTo("상담원 진짜 ***같네");
    }

    @Test
    @DisplayName("금칙어가 없는 메시지는 NORMAL, 원문 그대로")
    void normalMessageUnchanged() {
        String message = "환불 처리가 언제 되나요?";
        AbuseFilterResult result = abuseFilter.filter(message);

        assertThat(result.abuseStatus()).isEqualTo(AbuseStatus.NORMAL);
        assertThat(result.detected()).isFalse();
        assertThat(result.maskedContent()).isEqualTo(message);
    }

    @ParameterizedTest
    @ValueSource(strings = {"시 발", "ㅆ!발", "병.신", "개 새 끼"})
    @DisplayName("띄어쓰기·특수문자를 섞은 변형 표현도 감지")
    void variantsAreDetected(String message) {
        AbuseFilterResult result = abuseFilter.filter(message);

        assertThat(result.abuseStatus()).isEqualTo(AbuseStatus.ABUSIVE);
        assertThat(result.maskedContent()).isEqualTo("***");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "환불이 왜 이렇게 늦나요? 정말 화가 납니다",
            "다시 발송해 주세요",
            "이게 말이 됩니까? 책임자 바꿔주세요",
            "신제품 출시 발표는 언제인가요?",     // 출시발표 → 「시발」
            "질병 신고는 어디서 하나요?",          // 질병신고 → 「병신」
            "이미 친구한테 들었어요",               // 이미친구 → 「미친」
            "새끼손가락을 다쳤어요"                 // 「새끼」
    })
    @DisplayName("일반 항의·불만 문장은 NORMAL (오탐 방지)")
    void complaintIsNormal(String message) {
        AbuseFilterResult result = abuseFilter.filter(message);

        assertThat(result.abuseStatus()).isEqualTo(AbuseStatus.NORMAL);
        assertThat(result.maskedContent()).isEqualTo(message);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "이 미친 상담원아",        // 「이미친구」 예외와 앞부분이 같지만 욕설
            "시발 출시 언제 하냐",     // 「출시발표」 예외가 있어도 앞쪽 「시발」은 잡혀야 함
            "병신아 질병 신고 해"      // 「질병신고」 예외가 있어도 앞쪽 「병신」은 잡혀야 함
    })
    @DisplayName("예외 표현과 비슷해도 진짜 욕설은 ABUSIVE (예외가 너무 넓지 않은지)")
    void allowedPhrasesDoNotHideAbuse(String message) {
        AbuseFilterResult result = abuseFilter.filter(message);

        assertThat(result.abuseStatus()).isEqualTo(AbuseStatus.ABUSIVE);
    }
}
