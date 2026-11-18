package com.safedesk.sanction.dto;

import com.safedesk.sanction.entity.SanctionStatus;

import java.time.LocalDateTime;
import java.util.List;

public record SanctionDetailDTO(
    Long sanctionId,
    SanctionCustomerDetailDTO customer,
    long violationCount,
    int durationDays,
    LocalDateTime startsAt,
    LocalDateTime endsAt,
    SanctionStatus status,
    boolean releasable,
    SanctionReleaseDTO release,
    List<ViolationItemDTO> violations
) {
}
