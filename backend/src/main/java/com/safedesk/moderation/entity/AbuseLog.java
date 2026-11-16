package com.safedesk.moderation.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 폭언 원문 보관 (MOD-01, REQ-026, NFR-005).
 *
 * abuse_log 테이블 (schema.sql 참고)
 *   abuse_log_id     bigint AUTO_INCREMENT PK
 *   message_id       bigint NOT NULL UNIQUE   (FK → message)
 *   consultation_id  bigint NOT NULL          (FK → consultation)
 *   original_content text   NOT NULL          폭언 원문
 *   created_at       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
 *
 * ddl-auto: validate 이므로 컬럼 이름·타입이 schema.sql 과 맞아야 앱이 뜬다.
 * 원문은 누구도 수정·삭제할 수 없다 (NFR-005) → setter 를 만들지 않는다.
 */

@Entity
@Table(name = "abuse_log")
public class AbuseLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "abuse_log_id")
    private Long abuseLogId;

    @Column(name = "message_id", nullable = false, unique = true)
    private Long messageId;

    @Column(name = "consultation_id", nullable = false)
    private Long consultationId;

    @Column(name = "original_content", nullable = false, columnDefinition = "TEXT")
    private String originalContent;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected AbuseLog() {}

    public AbuseLog(Long messageId, Long consultationId, String originalContent) {
        this.messageId = messageId;
        this.consultationId = consultationId;
        this.originalContent = originalContent;
    }

    public Long getAbuseLogId() {
        return abuseLogId;
    }

    public Long getMessageId() {
        return messageId;
    }

    public Long getConsultationId() {
        return consultationId;
    }

    public String getOriginalContent() {
        return originalContent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
