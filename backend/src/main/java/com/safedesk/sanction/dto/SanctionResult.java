package com.safedesk.sanction.dto;

import com.safedesk.sanction.entity.Sanction;
import com.safedesk.sanction.entity.Violation;

import java.time.LocalDateTime;

/**
 * 위반 기록·자동 정지 결과 (docs/api/moderation.md MOD-04 응답의 data 부분).
 * MOD-03 이 이 값으로 종료 알림(CONSULTATION_TERMINATED)에 정지 일수·해제 일시를 담는다.
 *
 * @param violationId     위반 ID
 * @param customerId      고객 ID
 * @param totalViolations 고객의 누적 위반 횟수 (이번 위반 포함)
 * @param sanctionId      정지 ID
 * @param suspendDays     정지 일수
 * @param startAt         정지 시작 = 상담 종료 시각 (REQ-021)
 * @param endAt           해제 예정 일시 = startAt + suspendDays
 */
public record SanctionResult(
    Long violationId,
    Long customerId,
    long totalViolations,
    Long sanctionId,
    int suspendDays,
    LocalDateTime startAt,
    LocalDateTime endAt
) {

    /**
     * Violation·Sanction 으로 결과를 만든다.
     * 「새로 만든 경우」와 「이미 있던 경우」 둘 다 같은 코드로 결과를 만들 수 있다.
     */
    public static SanctionResult from(Violation violation, Sanction sanction, long totalViolations) {
        return new SanctionResult(
            violation.getViolationId(),
            violation.getCustomerId(),
            totalViolations,
            sanction.getSanctionId(),
            sanction.getSuspendDays(),
            sanction.getStartAt(),
            sanction.getEndAt()
        );
    }
}
