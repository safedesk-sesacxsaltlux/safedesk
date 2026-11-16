package com.safedesk.moderation.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 폭언 경고 (MOD-02 자동, MOD-06 수동).
 *
 * warning 테이블 (schema.sql 참고)
 *   warning_id      bigint AUTO_INCREMENT PK
 *   consultation_id bigint NOT NULL          (FK → consultation)
 *   abuse_log_id    bigint NULL UNIQUE       (FK → abuse_log. 수동 경고는 NULL)
 *   warning_type    varchar(20) NOT NULL     AUTO / MANUAL
 *   issued_by       bigint NULL              (FK → member. 수동 경고한 상담원, 자동은 NULL)
 *   created_at      datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
 *
 * 경고 기록은 수정하지 않는다 → setter 를 만들지 않는다.
 */
@Entity
@Table(name = "warning")
public class Warning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warning_id")
    private Long warningId;

    @Column(name = "consultation_id", nullable = false)
    private Long consultationId;

    // 같은 폭언 기록으로 경고가 두 번 생기지 않게 DB 에서도 막는다 (uk_warning_abuse_log_id)
    @Column(name = "abuse_log_id", unique = true)
    private Long abuseLogId;

    // STRING: DB 에 "AUTO" 글자로 저장. 없으면 0·1 숫자로 저장돼 CHECK 제약에 걸린다
    @Enumerated(EnumType.STRING)
    @Column(name = "warning_type", nullable = false, length = 20)
    private WarningType warningType;

    @Column(name = "issued_by")
    private Long issuedBy;

    // DB 기본값(CURRENT_TIMESTAMP)을 쓴다
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Warning() {}   // JPA 용

    private Warning(Long consultationId, Long abuseLogId, WarningType warningType, Long issuedBy) {
        this.consultationId = consultationId;
        this.abuseLogId = abuseLogId;
        this.warningType = warningType;
        this.issuedBy = issuedBy;
    }

    /** 자동 경고 (MOD-02). 경고한 상담원이 없으므로 issuedBy 는 null */
    public static Warning auto(Long consultationId, Long abuseLogId) {
        return new Warning(consultationId, abuseLogId, WarningType.AUTO, null);
    }

    // MOD-06 수동 경고를 만들 때 manual(consultationId, agentId) 를 여기에 추가한다 (abuseLogId 는 null)

    public Long getWarningId() {
        return warningId;
    }

    public Long getConsultationId() {
        return consultationId;
    }

    public Long getAbuseLogId() {
        return abuseLogId;
    }

    public WarningType getWarningType() {
        return warningType;
    }

    public Long getIssuedBy() {
        return issuedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
