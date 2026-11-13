package com.safedesk.ai.dto;

/* 보내는 질문 */
public record AiReplyRequest(Long chatId,
                             String content) {
}
