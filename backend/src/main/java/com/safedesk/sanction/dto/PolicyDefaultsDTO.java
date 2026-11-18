package com.safedesk.sanction.dto;

import com.safedesk.sanction.entity.SanctionPolicy;

public record PolicyDefaultsDTO(
    int warningThreshold,
    int autoCloseThreshold,
    SuspensionDaysDTO suspensionDays
) {

    public static PolicyDefaultsDTO from(SanctionPolicy policy) {
        return new PolicyDefaultsDTO(
            policy.getWarnThreshold(),
            policy.getCloseThreshold(),
            SuspensionDaysDTO.from(policy)
        );
    }
}
