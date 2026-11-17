package com.safedesk.chat.dto;

import com.safedesk.chat.entity.Message;

import java.time.LocalDateTime;

// CUS-02 응답 안의 메시지 한 개 (고객 질문 또는 봇 답변)
public record BotMessageItemDTO(Long messageId, String senderType, String content, LocalDateTime createdAt) {

    public static BotMessageItemDTO from(Message message) {
        return new BotMessageItemDTO(
            message.getMessageId(), message.getSenderType(), message.getContent(), message.getCreatedAt());
    }
}
