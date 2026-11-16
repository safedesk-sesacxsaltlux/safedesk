package com.safedesk.history.summary;

// 상담 요약 결과를 담는 record
public record ConsultationSummary(
        String inquiryCategory,
        String inquirySummary,
        String processSummary,
        String result
) {

    // 요약 생성에 실패했을 때 사용할 공통 문자열
    public static final String EMPTY_TEXT = "요약 없음";
}
