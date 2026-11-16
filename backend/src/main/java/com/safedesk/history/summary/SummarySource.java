package com.safedesk.history.summary;

import java.time.LocalDateTime;

// 요약을 만드는 데 필요한 재료. DB에서 읽어 이 형태로 모은 뒤 Generator에 넘긴다.
// faqQuestion: consultation.faq_id 가 가리키는 FAQ 의 대표 질문. 없으면 null
// hasFaqMatch: 상담 중 FAQ 가 매칭됐는지 (faq_id 유무). AI 단독 종료의 해결 여부 판정에 쓴다.
public record SummarySource(
        String status,
        String closedBy,
        String closeReason,
        LocalDateTime queuedAt,
        LocalDateTime acceptedAt,
        String existingCategory,
        String existingInquirySummary,
        String firstCustomerMessage,
        String faqQuestion,
        boolean hasFaqMatch,
        long abusiveCount
) {
}
