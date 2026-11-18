package com.safedesk.sanction.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 이용정지 (MOD-04에서 생성, ADM 수동 해제에서 released_* 를 채운다).
 * <p>
 * sanction 테이블 (schema.sql 참고)
 * sanction_id    bigint AUTO_INCREMENT PK
 * violation_id   bigint NOT NULL UNIQUE    (FK → violation. 위반 1건에 정지 1건, uk_sanction_violation_id)
 * customer_id    bigint NOT NULL           (FK → member)
 * suspend_days   int NOT NULL              CHECK 1~365
 * start_at       datetime NOT NULL         정지 시작 = 상담 종료 시각 (REQ-021)
 * end_at         datetime NOT NULL         해제 예정 = start_at + suspend_days, CHECK end_at > start_at
 * released_at    datetime NULL             수동 해제 일시
 * released_by    bigint NULL               해제한 관리자
 * release_reason varchar(255) NULL         해제 사유
 * created_at     datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
 * <p>
 * 정지 상태(SUSPENDED / RELEASED / EXPIRED)는 컬럼으로 저장하지 않고 계산한다 (contract.md SanctionStatus).
 */

@Entity
@Table(name = "sanction")
public class Sanction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sanction_id")
    private Long sanctionId;

    @Column(name = "violation_id", unique = true, nullable = false)
    private Long violationId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "suspend_days", nullable = false)
    private int suspendDays;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "released_by")
    private Long releasedBy;

    @Column(name = "release_reason")
    private String releaseReason;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Sanction() {
    }

    private Sanction(Long violationId, Long customerId, int suspendDays, LocalDateTime startAt) {
        Objects.requireNonNull(startAt, "startAt");
        this.violationId = violationId;
        this.customerId = customerId;
        this.suspendDays = suspendDays;
        this.startAt = startAt;
        this.endAt = startAt.plusDays(suspendDays);
    }

    public static Sanction of(Long violationId, Long customerId, int suspendDays, LocalDateTime startAt) {
        return new Sanction(violationId, customerId, suspendDays, startAt);
    }

    public SanctionStatus statusAt(LocalDateTime now) {
        return SanctionStatus.of(releasedAt, endAt, now);
    }

    public void release(Long adminId, String reason, LocalDateTime now) {
        this.releasedAt = now;
        this.releasedBy = adminId;
        this.releaseReason = reason;
    }


    public Long getSanctionId() {
        return sanctionId;
    }

    public Long getViolationId() {
        return violationId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public int getSuspendDays() {
        return suspendDays;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public LocalDateTime getReleasedAt() {
        return releasedAt;
    }

    public Long getReleasedBy() {
        return releasedBy;
    }

    public String getReleaseReason() {
        return releaseReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
