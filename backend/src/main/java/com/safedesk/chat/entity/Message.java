package com.safedesk.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name="message")
public class Message {

    @Id
    @Column(name="message_id")
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
}
