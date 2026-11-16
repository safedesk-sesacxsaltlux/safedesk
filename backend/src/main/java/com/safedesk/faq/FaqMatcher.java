package com.safedesk.faq;

import com.safedesk.faq.dto.FaqKeywordRow;
import com.safedesk.faq.dto.FaqMatch;
import com.safedesk.faq.repository.FaqKeywordRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * FAQ 키워드 매칭 (customer-chat.md CUS-02 5번).
 *
 * 공백을 지운 질문에 포함된 키워드 글자 수 합이 가장 큰 FAQ 를 고른다. 0점이면 매칭 없음.
 * 점수가 같으면 faq_id 가 작은 FAQ 를 고른다. (같은 질문에 항상 같은 결과가 나오도록)
 *
 * CUS-02(AI 실패 시 대체 답변, 문의 유형 저장)와 INT-01(요약 시 문의 유형 재확인)이 같은 기준을 쓴다.
 * 관리자가 FAQ 를 바꾸면 바로 반영되도록 캐시 없이 매번 조회한다.
 */
@Component
public class FaqMatcher {

    private final FaqKeywordRepository faqKeywordRepository;

    public FaqMatcher(FaqKeywordRepository faqKeywordRepository) {
        this.faqKeywordRepository = faqKeywordRepository;
    }

    /**
     * 문장에 가장 잘 맞는 FAQ 를 찾는다.
     *
     * @return 매칭된 FAQ. 문장이 비었거나 맞는 키워드가 없으면 empty
     */
    @Transactional(readOnly = true)
    public Optional<FaqMatch> match(String text) {
        String question = removeWhitespace(text);
        if (question.isEmpty()) {
            return Optional.empty();
        }

        // faqId 별 점수 합산. 키워드 하나가 여러 번 나와도 한 번만 센다.
        Map<Long, FaqMatch> scores = new LinkedHashMap<>();
        for (FaqKeywordRow row : faqKeywordRepository.findAllRows()) {
            String keyword = removeWhitespace(row.keyword());
            if (keyword.isEmpty() || !question.contains(keyword)) {
                continue;
            }
            scores.merge(
                    row.faqId(),
                    new FaqMatch(row.faqId(), row.category(), row.answer(), keyword.length()),
                    (prev, add) -> new FaqMatch(prev.faqId(), prev.category(), prev.answer(), prev.score() + add.score()));
        }

        return scores.values().stream()
                .max(Comparator.comparingInt(FaqMatch::score)
                        .thenComparing(FaqMatch::faqId, Comparator.reverseOrder()));
    }

    /**
     * 문장에 맞는 FAQ 의 문의 유형(category)을 돌려준다. INT-01 상담 요약용.
     *
     * @return 문의 유형. 매칭되지 않으면 null
     */
    @Transactional(readOnly = true)
    public String findCategory(String text) {
        return match(text).map(FaqMatch::category).orElse(null);
    }

    private static String removeWhitespace(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\s+", "");
    }
}
