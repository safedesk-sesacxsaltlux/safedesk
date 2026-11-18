package com.safedesk.sanction.dto;

import com.safedesk.sanction.entity.SanctionPolicy;

import java.time.LocalDateTime;

public record PolicyVersionDTO(
    Long policyVersion,
    int warningThreshold,
    int autoCloseThreshold,
    SuspensionDaysDTO suspensionDays,
    LocalDateTime updatedAt,
    PolicyAdminDTO updatedBy
) {

    public static PolicyVersionDTO of(SanctionPolicy policy, PolicyAdminDTO updatedBy) {
        return new PolicyVersionDTO(
            policy.getPolicyId(),
            policy.getWarnThreshold(),
            policy.getCloseThreshold(),
            SuspensionDaysDTO.from(policy),
            policy.getCreatedAt(),
            updatedBy
        );
    }
}
