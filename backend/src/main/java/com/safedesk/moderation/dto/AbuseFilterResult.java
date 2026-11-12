package com.safedesk.moderation.dto;

import com.safedesk.moderation.entity.AbuseStatus;

/**
 * AbuseFilter 판정 결과 (docs/api/moderation.md MOD-01 응답의 data 부분).
 *
 * @param abuseStatus   NORMAL 또는 ABUSIVE
 * @param maskedContent NORMAL 이면 원문 그대로, ABUSIVE 이면 금칙어 구간만 *** 로 치환한 문장
 */
public record AbuseFilterResult(AbuseStatus abuseStatus, String maskedContent) {

    public boolean detected() {
        return abuseStatus == AbuseStatus.ABUSIVE;
    }

    public static AbuseFilterResult normal(String original) {
        return new AbuseFilterResult(AbuseStatus.NORMAL, original);
    }

    public static AbuseFilterResult abusive(String masked) {
        return new AbuseFilterResult(AbuseStatus.ABUSIVE, masked);
    }
}
