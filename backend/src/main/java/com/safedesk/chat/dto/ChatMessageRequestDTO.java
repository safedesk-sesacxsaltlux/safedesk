package com.safedesk.chat.dto;

public class ChatMessageRequestDTO {

    private String senderType;
    private Long senderId;
    private String messageContent;
    private String abuseStatus;

    public String getSenderType() {
        return senderType;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public String getAbuseStatus() {
        return abuseStatus;
    }

    public void setAbuseStatus(String abuseStatus) {
        this.abuseStatus = abuseStatus;
    }
}
