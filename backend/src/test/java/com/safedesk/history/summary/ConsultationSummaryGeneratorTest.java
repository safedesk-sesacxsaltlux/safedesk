package com.safedesk.history.summary;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * INT-01 저장 규칙 중 FAQ 매칭과 관련된 부분 (docs/api/internal.md)
 */
class ConsultationSummaryGeneratorTest {

    private final ConsultationSummaryGenerator generator = new ConsultationSummaryGenerator();

    @Test
    @DisplayName("FAQ 가 매칭된 AI 단독 종료: 저장된 문의 유형, FAQ 대표 질문, RESOLVED")
    void faqMatched() {
        ConsultationSummary summary = generator.generate(aiOnly("대중교통", "분실물은 어떻게 찾나요?", true, "안녕하세요"));

        assertThat(summary.inquiryCategory()).isEqualTo("대중교통");
        assertThat(summary.inquirySummary()).isEqualTo("분실물은 어떻게 찾나요?");
        assertThat(summary.result()).isEqualTo("RESOLVED");
    }

    @Test
    @DisplayName("FAQ 매칭 없는 AI 단독 종료: 기타, 첫 고객 메시지, UNRESOLVED")
    void noFaqMatch() {
        ConsultationSummary summary = generator.generate(aiOnly(null, null, false, "오늘 날씨 어때요"));

        assertThat(summary.inquiryCategory()).isEqualTo(ConsultationSummaryGenerator.DEFAULT_CATEGORY);
        assertThat(summary.inquirySummary()).isEqualTo("오늘 날씨 어때요");
        assertThat(summary.result()).isEqualTo("UNRESOLVED");
    }

    @Test
    @DisplayName("첫 고객 메시지가 50자를 넘으면 50자 + … 로 자른다")
    void truncateFirstMessage() {
        String longMessage = "가".repeat(60);

        ConsultationSummary summary = generator.generate(aiOnly(null, null, false, longMessage));

        assertThat(summary.inquirySummary()).isEqualTo("가".repeat(50) + "…");
    }

    @Test
    @DisplayName("FAQ 대표 질문이 컬럼 길이(100자)를 넘지 않게 자른다")
    void truncateFaqQuestion() {
        String longQuestion = "나".repeat(150);

        ConsultationSummary summary = generator.generate(aiOnly("일반행정", longQuestion, true, "질문"));

        assertThat(summary.inquirySummary()).hasSize(100).endsWith("…");
    }

    @Test
    @DisplayName("고객 메시지가 없으면 문의 내용 없음")
    void noCustomerMessage() {
        ConsultationSummary summary = generator.generate(aiOnly(null, null, false, null));

        assertThat(summary.inquirySummary()).isEqualTo(ConsultationSummaryGenerator.NO_INQUIRY_TEXT);
    }

    private static SummarySource aiOnly(String existingCategory, String faqQuestion,
                                        boolean hasFaqMatch, String firstCustomerMessage) {
        return new SummarySource(
                "CLOSED", "CUSTOMER", "NORMAL",
                null, null,
                existingCategory, null, firstCustomerMessage,
                faqQuestion, hasFaqMatch,
                0
        );
    }
}
