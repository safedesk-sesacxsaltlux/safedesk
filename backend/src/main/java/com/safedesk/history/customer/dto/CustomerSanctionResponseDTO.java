package com.safedesk.history.customer.dto;

import java.time.LocalDateTime;
import java.util.List;

// HIS-02 내 이용 상태(제재) 응답 (docs/api/history.md)
public record CustomerSanctionResponseDTO(
        String sanctionStatus,          // SUSPENDED | NONE
        boolean canStartConsultation,
        long totalViolationCount,       // 누적 위반 횟수 (정지가 풀려도 줄지 않음)
        CurrentSanction currentSanction, // 정지 중이 아니면 null
        List<PolicyGuide> policyGuide   // 최신 제재 정책 기준 (1회·2회·3회 이상)
) {

    public record CurrentSanction(
            long sanctionId,
            int suspendDays,
            LocalDateTime startAt,
            LocalDateTime endAt,
            long remainingDays,         // 해제까지 남은 일수 (올림) → 화면의 D-N
            String reason
    ) {
    }

    public record PolicyGuide(int violationCount, int suspendDays, boolean orMore) {
    }
}
