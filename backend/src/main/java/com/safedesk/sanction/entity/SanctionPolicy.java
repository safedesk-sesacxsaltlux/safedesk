package com.safedesk.sanction.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 제재 정책 (sanction_policy). 경고·자동 종료 기준과 정지 일수.
 *
 * 관리자 정책 화면(ADM-01·02, 장건웅 님)과 MOD-04(정지 일수)에서도 쓰므로 sanction 패키지에 둔다.
 * 정책은 수정하지 않고 새 행을 추가하는 방식이다. 상담은 시작할 때의 policy_id 를 들고 있다.
 *
 * sanction_policy 테이블 (schema.sql 참고)
 *   policy_id       bigint AUTO_INCREMENT PK
 *   warn_threshold  int NOT NULL    경고 발송 기준(N회째)   기본 1
 *   close_threshold int NOT NULL    자동 종료 기준(N회)     기본 2
 *   first_days      int NOT NULL    누적 1회 정지 일수      기본 7
 *   second_days     int NOT NULL    누적 2회 정지 일수      기본 30
 *   third_days      int NOT NULL    누적 3회 이상 정지 일수 기본 90
 *   change_note     varchar(255) NULL
 *   created_by      bigint NULL
 *   created_at      datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
 */
@Entity
@Table(name = "sanction_policy")
public class SanctionPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    private Long policyId;

    @Column(name = "warn_threshold", nullable = false)
    private int warnThreshold;

    @Column(name = "close_threshold", nullable = false)
    private int closeThreshold;

    // 정지 일수는 MOD-04(자동 제재)에서 쓴다
    @Column(name = "first_days", nullable = false)
    private int firstDays;

    @Column(name = "second_days", nullable = false)
    private int secondDays;

    @Column(name = "third_days", nullable = false)
    private int thirdDays;

    @Column(name = "change_note")
    private String changeNote;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected SanctionPolicy() {}   // JPA 용

    public static SanctionPolicy create(int warnThreshold, int closeThreshold, int firstDays, int secondDays,
                                        int thirdDays, Long createdBy) {
        SanctionPolicy policy = new SanctionPolicy();
        policy.warnThreshold = warnThreshold;
        policy.closeThreshold = closeThreshold;
        policy.firstDays = firstDays;
        policy.secondDays = secondDays;
        policy.thirdDays = thirdDays;
        policy.createdBy = createdBy;
        policy.createdAt = LocalDateTime.now().withNano(0);
        return policy;
    }

    public Long getPolicyId() {
        return policyId;
    }

    public int getWarnThreshold() {
        return warnThreshold;
    }

    public int getCloseThreshold() {
        return closeThreshold;
    }

    public int getFirstDays() {
        return firstDays;
    }

    public int getSecondDays() {
        return secondDays;
    }

    public int getThirdDays() {
        return thirdDays;
    }

    public String getChangeNote() {
        return changeNote;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
