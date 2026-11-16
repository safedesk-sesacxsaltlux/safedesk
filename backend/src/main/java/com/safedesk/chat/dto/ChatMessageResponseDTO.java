package com.safedesk.chat.dto;

import com.safedesk.chat.entity.Message;

import java.time.LocalDateTime;

/**
 * WebSocket /topic/chats/{consultationId} 로 발행하는 채팅 메시지 (contract.md WebSocket 「MESSAGE」).
 * <p>
 * 지금까지는 고객이 보낸 요청 DTO 를 그대로 다시 발행해서 원문이 상담원에게 갔다.
 * 이제 「저장된 메시지(마스킹본)」로 이 DTO 를 만들어 발행한다.
 * ⚠️ 원문 필드는 절대 넣지 않는다 (NFR-007).
 */
public class ChatMessageResponseDTO {

    private final String type = "MESSAGE";
    private long messageId;
    private long consultationId;
    private String senderType;
    private String content;
    private String abuseStatus;
    private LocalDateTime createdAt;

    public static ChatMessageResponseDTO from(Message message) {
        ChatMessageResponseDTO dto = new ChatMessageResponseDTO();
        dto.messageId = message.getMessageId();
        dto.consultationId = message.getConsultationId();
        dto.senderType = message.getSenderType();
        dto.content = message.getContent();
        dto.abuseStatus = message.getAbuseStatus();
        dto.createdAt = message.getCreatedAt();

        return dto;
    }

    public String getType() {
        return type;
    }

    public long getMessageId() {
        return messageId;
    }

    public long getConsultationId() {
        return consultationId;
    }

    public String getSenderType() {
        return senderType;
    }

    public String getContent() {
        return content;
    }

    public String getAbuseStatus() {
        return abuseStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
