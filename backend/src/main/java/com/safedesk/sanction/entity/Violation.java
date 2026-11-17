package com.safedesk.sanction.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 위반 이력 (MOD-04). 폭언으로 종료된 상담 1건 = 위반 1회 (policy.md 5장, REQ-020).
 *
 * violation 테이블 (schema.sql 참고)
 *   violation_id    bigint AUTO_INCREMENT PK
 *   customer_id     bigint NOT NULL          (FK → member)
 *   consultation_id bigint NOT NULL UNIQUE   (FK → consultation. 상담 1건에 위반 1건, uk_violation_consultation_id)
 *   created_at      datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
 *
 * 위반 기록은 수정·삭제하지 않는다 → setter 를 만들지 않는다. (정지를 수동 해제해도 위반은 남는다, REQ-025)
 */
@Entity
@Table(name = "violation")
public class Violation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "violation_id")
    private Long violationId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "consultation_id", nullable = false, unique = true)
    private Long consultationId;

    // 저장 직후 createdAt은 null이라 정지 시작 시각은 closedAt을 쓴다
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;


    protected Violation() {}

    private Violation(Long customerId, Long consultationId){
        this.customerId = customerId;
        this.consultationId = consultationId;
    }

    public static Violation of(Long customerId, Long consultationId){
        return new Violation(customerId, consultationId);
    }


    public Long getViolationId() {
        return violationId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Long getConsultationId() {
        return consultationId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
