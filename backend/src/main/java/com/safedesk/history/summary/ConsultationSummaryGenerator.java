package com.safedesk.history.summary;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

// 요약 템플릿 규칙 (docs/api/internal.md INT-01 「저장 규칙」)
// DB를 모르는 순수 로직만 둔다. 그래서 DB 없이 단위 테스트할 수 있다.
@Component
// 조건문을 이용해 요약 생성
public class ConsultationSummaryGenerator {
    static final String DEFAULT_CATEGORY = "기타";
    static final String NO_INQUIRY_TEXT = "문의 내용 없음";
    private static final int FIRST_MESSAGE_MAX_LENGTH = 50;
    private static final int FAQ_QUESTION_MAX_LENGTH = 99;   // inquiry_summary varchar(100) - 말줄임 1자
    private static final String ELLIPSIS = "…";

    public ConsultationSummary generate(SummarySource source) {
        return new ConsultationSummary(
                inquiryCategory(source),
                inquirySummary(source),
                processSummary(source),
                result(source)
        );
    }


    // 문의 유형: 상담 중 저장된 값(CUS-02 6단계) → "기타"
    private String inquiryCategory(SummarySource source) {
        if (hasText(source.existingCategory())) {
            return source.existingCategory();
        }
        return DEFAULT_CATEGORY;
    }

    // 문의 요약: 저장된 값 → 매칭된 FAQ 대표 질문 → 첫 고객 메시지 앞 50자
    private String inquirySummary(SummarySource source) {
        if (hasText(source.existingInquirySummary())) {
            return source.existingInquirySummary();
        }
        if (hasText(source.faqQuestion())) {
            return truncate(source.faqQuestion().strip(), FAQ_QUESTION_MAX_LENGTH);
        }
        if (!hasText(source.firstCustomerMessage())) {
            return NO_INQUIRY_TEXT;
        }
        return truncate(source.firstCustomerMessage().strip(), FIRST_MESSAGE_MAX_LENGTH);
    }

    // maxLength 글자까지 남기고, 넘치면 "…" 를 붙인다.
    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + ELLIPSIS;
    }

    // 상담이 어떤 순서로 진행되었는지 요약
    private String processSummary(SummarySource source) {

        // 상담 진행 단계를 저장할 리스트 생성
        List<String> steps = new ArrayList<>();

        steps.add("AI 응대");

        if(source.queuedAt() != null) {
            steps.add("상담원 연결 요청");
        }
        if(source.acceptedAt() != null) {
            steps.add("상담원 상담");
        }
        steps.add(closingPhrase(source.closeReason(), source.closedBy()));

        String summary = String.join(" → ", steps);
        if(source.abusiveCount() > 0) {
            summary += "(폭언 감지 " + source.abusiveCount() + "회)";
        }
        return summary;
    }

    private String closingPhrase(String closeReason, String closedBy) {
        if (closeReason == null) {
            return "상담 종료";
        }
        return switch (closeReason) {
            case "NORMAL" ->
                switch(closedBy == null ? "" : closedBy) {
                    case "CUSTOMER" -> "고객 종료";
                    case "AGENT" -> "상담원 종료";
                    default -> "상담 종료";
                };
            case "QUEUE_CANCEL" -> "대기 중 취소";
            case "ABUSE_AUTO" -> "반복 폭언으로 자동 종료";
            case "ABUSE_MANUAL" -> "폭언으로 상담원 종료";
            default -> "상담 종료";
        };
    }

    private String result(SummarySource source) {
        if ("TERMINATED".equals(source.status())) {
            return "TERMINATED";
        }
        if ("QUEUE_CANCEL".equals(source.closeReason())) {
            return "UNRESOLVED";
        }
        if (source.acceptedAt() == null) {
            // AI 단독 종료: 상담 중 FAQ 가 매칭됐으면(faq_id 있음) 해결로 본다.
            return source.hasFaqMatch() ? "RESOLVED" : "UNRESOLVED";
        }
        return "RESOLVED";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
