package com.safedesk.chat.dto;

public class ChatMessageRequestDTO {

    private long consultationId;
    private String senderType;
    private long senderMemberId;
    private String messageContent;
    private String abuseStatus;

    public long getConsultationId() {
        return consultationId;
    }

    public void setConsultationId(long consultationId) {
        this.consultationId = consultationId;
    }

    public String getSenderType() {
        return senderType;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }

    public long getSenderMemberId() {
        return senderMemberId;
    }

    public void setSenderMemberId(long senderMemberId) {
        this.senderMemberId = senderMemberId;
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
