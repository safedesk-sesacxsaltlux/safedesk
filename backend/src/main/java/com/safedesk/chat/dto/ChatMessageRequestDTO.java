package com.safedesk.chat.dto;

/**
 * SEND /app/chats/{chatId}/messages 본문 (contract.md WebSocket 「보내기」).
 *
 * { "content": "..." }
 *
 * 발신자(senderType·senderId)는 서버가 CONNECT 토큰으로 정한다. abuseStatus 도 서버가 정한다.
 * 클라이언트가 그런 필드를 보내도 받지 않는다 (NFR-004).
 */
public class ChatMessageRequestDTO {

    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
