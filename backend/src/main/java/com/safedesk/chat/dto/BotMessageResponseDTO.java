package com.safedesk.chat.dto;

// CUS-02 응답. handoffSuggested 가 true 면 화면이 「상담원 연결」 버튼을 보여 준다
public record BotMessageResponseDTO(
    BotMessageItemDTO customerMessage,
    BotMessageItemDTO botMessage,
    String answerSource,
    Long faqId,
    boolean handoffSuggested,
    String handoffReason
) {
}
