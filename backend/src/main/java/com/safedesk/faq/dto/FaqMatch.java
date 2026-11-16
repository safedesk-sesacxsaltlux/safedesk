package com.safedesk.faq.dto;

/**
 * FAQ 키워드 매칭 결과.
 *
 * @param faqId    매칭된 FAQ (CUS-02 반복 질문 판정에 쓴다)
 * @param category 문의 유형 (CONSULTATION.inquiry_category 에 저장)
 * @param answer   FAQ 대체 답변 (AI 실패 시 고객에게 보낸다)
 * @param score    질문에 포함된 키워드 글자 수 합
 */
public record FaqMatch(Long faqId, String category, String answer, int score) {
}
