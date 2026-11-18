package com.safedesk.sanction.dto;

import com.safedesk.sanction.entity.SanctionStatus;
import com.safedesk.sanction.repository.SanctionListRow;

import java.time.LocalDateTime;

// API-ADM-04 제재 목록 한 행
public record SanctionListItemDTO(
    Long sanctionId,
    SanctionCustomerDTO customer,
    long violationCount,
    LocalDateTime lastViolatedAt,
    int durationDays,
    LocalDateTime startsAt,
    LocalDateTime endsAt,
    SanctionStatus status
) {

    public static SanctionListItemDTO of(SanctionListRow row, LocalDateTime now) {
        return new SanctionListItemDTO(
            row.getSanctionId(),
            new SanctionCustomerDTO(row.getCustomerId(), row.getLoginId(), row.getName()),
            row.getViolationCount(),
            row.getViolatedAt(),
            row.getSuspendDays(),
            row.getStartAt(),
            row.getEndAt(),
            SanctionStatus.of(row.getReleasedAt(), row.getEndAt(), now)
        );
    }
}
