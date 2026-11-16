package com.safedesk.faq;

import com.safedesk.faq.dto.FaqKeywordRow;
import com.safedesk.faq.dto.FaqMatch;
import com.safedesk.faq.repository.FaqKeywordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * DB 없이 도는 단위 테스트. 키워드는 schema.sql 시드 일부를 그대로 쓴다.
 */
class FaqMatcherTest {

    private FaqKeywordRepository repository;
    private FaqMatcher faqMatcher;

    @BeforeEach
    void setUp() {
        repository = mock(FaqKeywordRepository.class);
        when(repository.findAllRows()).thenReturn(List.of(
                // 1 상하수도: 수도요금 납부 방법
                row(1L, "상하수도", "수도세"), row(1L, "상하수도", "납부방법"), row(1L, "상하수도", "요금납부"),
                row(1L, "상하수도", "편의점"), row(1L, "상하수도", "편의점납부"),
                // 2 상하수도: 수도요금 조회
                row(2L, "상하수도", "요금조회"), row(2L, "상하수도", "조회"), row(2L, "상하수도", "아리수"),
                // 9 상하수도: 누수 감액
                row(9L, "상하수도", "누수"), row(9L, "상하수도", "많이나왔"),
                // 10 일반행정: 지방세 납부
                row(10L, "일반행정", "위택스"), row(10L, "일반행정", "자동차세"),
                // 22 대중교통: 분실물
                row(22L, "대중교통", "분실물"), row(22L, "대중교통", "잃어버")
        ));
        faqMatcher = new FaqMatcher(repository);
    }

    @Test
    @DisplayName("띄어쓰기가 있어도 공백을 지우고 키워드를 찾는다")
    void ignoresWhitespace() {
        Optional<FaqMatch> result = faqMatcher.match("수도요금 납부 방법 알려주세요");

        assertThat(result).isPresent();
        assertThat(result.get().faqId()).isEqualTo(1L);
        assertThat(result.get().category()).isEqualTo("상하수도");
        assertThat(result.get().answer()).isEqualTo("answer-1");
    }

    @Test
    @DisplayName("포함된 키워드 글자 수 합이 가장 큰 FAQ 를 고른다")
    void picksHighestScore() {
        // FAQ 1: 편의점(3) + 편의점납부(5) = 8 / FAQ 2: 조회(2) = 2
        Optional<FaqMatch> result = faqMatcher.match("편의점납부 조회 되나요");

        assertThat(result).get().extracting(FaqMatch::faqId, FaqMatch::score).containsExactly(1L, 8);
    }

    @Test
    @DisplayName("점수가 같으면 faq_id 가 작은 FAQ 를 고른다")
    void tieBreaksBySmallerFaqId() {
        // FAQ 2: 아리수(3) / FAQ 10: 위택스(3)
        Optional<FaqMatch> result = faqMatcher.match("위택스랑 아리수 차이가 뭐예요");

        assertThat(result).get().extracting(FaqMatch::faqId).isEqualTo(2L);
    }

    @Test
    @DisplayName("맞는 키워드가 없으면 매칭 없음")
    void noMatch() {
        assertThat(faqMatcher.match("오늘 날씨 어때요")).isEmpty();
        assertThat(faqMatcher.findCategory("오늘 날씨 어때요")).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\n\t"})
    @DisplayName("빈 문장은 DB 를 보지 않고 매칭 없음")
    void blankText(String text) {
        assertThat(faqMatcher.match(text)).isEmpty();
        assertThat(faqMatcher.findCategory(text)).isNull();
    }

    @Test
    @DisplayName("findCategory 는 매칭된 FAQ 의 문의 유형을 돌려준다")
    void findCategory() {
        assertThat(faqMatcher.findCategory("지하철에서 지갑을 잃어버렸어요")).isEqualTo("대중교통");
        assertThat(faqMatcher.findCategory("누수 때문에 요금이 많이 나왔어요")).isEqualTo("상하수도");
    }

    private static FaqKeywordRow row(Long faqId, String category, String keyword) {
        return new FaqKeywordRow(faqId, category, "answer-" + faqId, keyword);
    }
}
