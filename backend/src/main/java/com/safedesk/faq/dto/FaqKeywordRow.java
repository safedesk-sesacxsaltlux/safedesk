package com.safedesk.faq.dto;

/**
 * 매칭용으로 faq_keyword 와 faq 를 조인한 한 줄. FAQ 하나에 키워드가 여러 개면 여러 줄이 나온다.
 */
public record FaqKeywordRow(Long faqId, String category, String question, String answer, String keyword) {
}
