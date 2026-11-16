package com.safedesk.moderation.dto;

/**
 * 자동 경고 결과 (docs/api/moderation.md MOD-02 응답의 data 부분).
 * ChatService 가 이 값을 보고 경고 알림 발행(지금)과 자동 종료(MOD-03)를 결정한다.
 *
 * @param consultationId  상담 ID
 * @param warningId       이번에 저장된 경고 ID
 * @param warningCount    이 상담의 경고 횟수 (AUTO + MANUAL, 이번 경고 포함)
 * @param closeThreshold  상담에 적용된 정책의 자동 종료 기준
 * @param shouldTerminate warningCount >= closeThreshold 이면 true → MOD-03 자동 종료 대상
 */
public record WarningResult(
    Long consultationId,
    Long warningId,
    long warningCount,
    int closeThreshold,
    boolean shouldTerminate
) {
}
