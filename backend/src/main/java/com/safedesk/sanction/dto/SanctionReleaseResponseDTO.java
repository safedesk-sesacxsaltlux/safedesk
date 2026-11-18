package com.safedesk.sanction.dto;

import com.safedesk.sanction.entity.SanctionStatus;

public record SanctionReleaseResponseDTO(
    Long sanctionId,
    SanctionStatus status,
    long violationCount,
    SanctionReleaseDTO release
) {
}
