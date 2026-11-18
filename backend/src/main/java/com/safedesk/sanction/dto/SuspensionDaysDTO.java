package com.safedesk.sanction.dto;

import com.safedesk.sanction.entity.SanctionPolicy;

public record SuspensionDaysDTO(
    int first,
    int second,
    int thirdOrMore
) {

    public static SuspensionDaysDTO from(SanctionPolicy policy) {
        return new SuspensionDaysDTO(
            policy.getFirstDays(),
            policy.getSecondDays(),
            policy.getThirdDays()
        );
    }
}
