package com.safedesk.sanction.entity;

import java.time.LocalDateTime;

// 제재 상태. 컬럼으로 저장하지 않고 해제·종료 시각으로 계산한다 (sanction.md API-ADM-04 상태 값)
public enum SanctionStatus {
    SUSPENDED,   // 정지 중: 수동 해제 안 됨, 현재 시각 < end_at
    RELEASED,    // 해제됨: 관리자가 수동 해제함
    EXPIRED;     // 기간 만료: 수동 해제 안 됨, 현재 시각 >= end_at

    // 엔티티(Sanction.statusAt)와 목록 조회 결과(엔티티가 아닌 조회 행)가 같은 규칙을 쓰도록 여기 한 곳에 둔다
    public static SanctionStatus of(LocalDateTime releasedAt, LocalDateTime endAt, LocalDateTime now) {
        if (releasedAt != null) {
            return RELEASED;
        }
        if (!endAt.isAfter(now)) {
            return EXPIRED;
        }
        return SUSPENDED;
    }
}
