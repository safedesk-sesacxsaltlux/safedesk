package com.safedesk.chat.dto;

import com.safedesk.chat.entity.Message;

import java.time.LocalDateTime;

/**
 * API-004 메시지 내역·API-007 AI 응대 내역의 한 줄.
 * content 는 저장된 마스킹본이다. 폭언 원문(abuse_log)은 넣지 않는다 (NFR-005).
 */
public record ChatHistoryResponseDTO(
    long messageId,
    long consultationId,
    Long senderId,
    String senderType,
    String content,
    String abuseStatus,
    LocalDateTime createdAt
) {

    public static ChatHistoryResponseDTO from(Message message) {
        return new ChatHistoryResponseDTO(
            message.getMessageId(),
            message.getConsultationId(),
            message.getSenderId(),
            message.getSenderType(),
            message.getContent(),
            message.getAbuseStatus(),
            message.getCreatedAt()
        );
    }
}
