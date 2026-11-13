package com.safedesk.chat.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name="message")
public class Message {

    @Id
    @Column(name="message_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long messageId;

    @Column(name="content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name="sender_type", nullable = false, length = 20)
    private String senderType;

    @Column(name="abuse_status", nullable = true, length = 20)
    private String abuseStatus;

    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name="consultation_id", nullable = false)
    private long consultationId;

    @Column(name="sender_id", nullable = false)
    private long senderId;

    public long getMessageId() {
        return messageId;
    }

    public String getContent() {
        return content;
    }

    public String getSenderType() {
        return senderType;
    }

    public String getAbuseStatus() {
        return abuseStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public long getConsultationId() {
        return consultationId;
    }

    public long getSenderId() {
        return senderId;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }

    public void setAbuseStatus(String abuseStatus) {
        this.abuseStatus = abuseStatus;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setConsultationId(long consultationId) {
        this.consultationId = consultationId;
    }

    public void setSenderId(long senderId) {
        this.senderId = senderId;
    }
}
