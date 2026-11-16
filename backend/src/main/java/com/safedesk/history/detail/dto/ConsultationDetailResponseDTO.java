package com.safedesk.history.detail.dto;

import java.time.LocalDateTime;
import java.util.List;

// HIS-06 상담 상세 응답. JSON 이 { consultation, summary, messages } 세 덩어리라
// 바깥 record 안에 안쪽 record 3개를 둔다.
public record ConsultationDetailResponseDTO(
    ConsultationInfo consultation,
    SummaryInfo summary,
    List<MessageInfo> messages
) {

    public record ConsultationInfo(
        long consultationId,
        String consultationNo,      // "C-000123"
        String status,
        String customerLoginId,
        String agentName,           // AI 단독 종료면 null
        LocalDateTime startedAt,
        LocalDateTime queuedAt,
        LocalDateTime acceptedAt,
        LocalDateTime closedAt,
        String closedBy,
        String closeReason,
        long abuseCount,
        boolean violationRecorded
    ) {
    }

    public record SummaryInfo(
        String summaryStatus,       // DONE | NONE
        String inquiryCategory,
        String inquirySummary,
        String processSummary,
        String result
    ) {
    }

    public record MessageInfo(
        long messageId,
        String senderType,
        String senderName,
        String content,             // 역할에 따라 원문 또는 마스킹본
        String abuseStatus,         // 폭언이면 "ABUSIVE", 아니면 null
        Integer warningNo,          // 몇 번째 경고인지. 없으면 null 이라 Integer
        LocalDateTime createdAt
    ) {
    }
}

