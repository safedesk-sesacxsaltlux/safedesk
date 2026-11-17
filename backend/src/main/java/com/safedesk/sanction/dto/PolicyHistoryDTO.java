package com.safedesk.sanction.dto;

import com.safedesk.sanction.entity.SanctionPolicy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public record PolicyHistoryDTO(
    Long policyVersion,
    LocalDateTime changedAt,
    PolicyAdminDTO changedBy,
    List<PolicyChangeDTO> changes
) {

    public static PolicyHistoryDTO of(SanctionPolicy policy, SanctionPolicy previous, PolicyAdminDTO changedBy) {
        return new PolicyHistoryDTO(
            policy.getPolicyId(),
            policy.getCreatedAt(),
            changedBy,
            changesBetween(previous, policy)
        );
    }

    private static List<PolicyChangeDTO> changesBetween(SanctionPolicy before, SanctionPolicy after) {
        List<PolicyChangeDTO> changes = new ArrayList<>();
        if (before == null) {
            return changes;
        }
        addIfChanged(changes, "warningThreshold", "경고 발송 기준",
            before.getWarnThreshold(), after.getWarnThreshold());
        addIfChanged(changes, "autoCloseThreshold", "자동 종료 기준",
            before.getCloseThreshold(), after.getCloseThreshold());
        addIfChanged(changes, "suspensionDays.first", "누적 1회 정지 기간",
            before.getFirstDays(), after.getFirstDays());
        addIfChanged(changes, "suspensionDays.second", "누적 2회 정지 기간",
            before.getSecondDays(), after.getSecondDays());
        addIfChanged(changes, "suspensionDays.thirdOrMore", "누적 3회 이상 정지 기간",
            before.getThirdDays(), after.getThirdDays());
        return changes;
    }

    private static void addIfChanged(List<PolicyChangeDTO> changes, String field, String label,
                                     int before, int after) {
        if (before != after) {
            changes.add(new PolicyChangeDTO(field, label, String.valueOf(before), String.valueOf(after)));
        }
    }
}
