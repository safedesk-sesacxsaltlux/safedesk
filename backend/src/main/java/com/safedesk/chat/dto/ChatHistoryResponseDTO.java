package com.safedesk.chat.dto;

import com.safedesk.chat.entity.Message;

import java.time.LocalDateTime;

    public class ChatHistoryResponseDTO {

        private long messageId;
        private long consultationId;
        private Long senderId;
        private String senderType;
        private String content;
        private String abuseStatus;
        private LocalDateTime createdAt;

        public static ChatHistoryResponseDTO from(Message message) {
            ChatHistoryResponseDTO dto = new ChatHistoryResponseDTO();

            dto.messageId = message.getMessageId();
            dto.consultationId = message.getConsultationId();
            dto.senderId = message.getSenderId();
            dto.senderType = message.getSenderType();
            dto.content = message.getContent();
            dto.abuseStatus = message.getAbuseStatus();
            dto.createdAt = message.getCreatedAt();

            return dto;
        }

        public long getMessageId() {
            return messageId;
        }

        public long getConsultationId() {
            return consultationId;
        }

        public Long getSenderId() {
            return senderId;
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


