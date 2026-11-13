package com.safedesk.moderation.entity;

/**
 * 폭언 판정 결과. message.abuse_status 의 CHECK 제약(NORMAL, ABUSIVE)과 값이 같아야 한다.
 */
public enum AbuseStatus {
    NORMAL,
    ABUSIVE
}
