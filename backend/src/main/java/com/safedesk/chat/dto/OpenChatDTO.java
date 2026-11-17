package com.safedesk.chat.dto;

// CUS-01 409 CHAT_ALREADY_OPEN 의 data. 화면이 기존 상담으로 이동할 때 쓴다.
public record OpenChatDTO(Long chatId, String status) {

}
