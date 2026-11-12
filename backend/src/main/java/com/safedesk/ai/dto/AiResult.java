package com.safedesk.ai.dto;

import com.safedesk.ai.AiFailureType;

/* 돌려받는 결과 */
public record AiResult( boolean success,
                        String content,
                        Long faqId,
                        long latencyMs,
                        AiFailureType failureType) {

    public static AiResult success(String content, long latencyMs) {
        return new AiResult(true, content, null, latencyMs, null);
    }
    public static AiResult failure(AiFailureType type, long latencyMs) {
        return new AiResult(false, null, null, latencyMs, type);
    }
}
