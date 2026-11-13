package com.safedesk.chat.dto;

public class ChatMessageRequestDTO {

    private String senderType;
    private long senderId;
    private String messageContent;
    private String abuseStatus;

    public String getSenderType() {
        return senderType;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }

    public long getSenderId() {
        return senderId;
    }

    public void setSenderId(long senderId) {
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
