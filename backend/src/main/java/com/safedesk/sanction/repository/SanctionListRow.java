package com.safedesk.sanction.repository;

import java.time.LocalDateTime;

// API-ADM-04 목록 조회 결과 한 행 (sanction + member + violation 조인).
public interface SanctionListRow {

    Long getSanctionId();

    Long getCustomerId();

    String getLoginId();

    String getName();

    Long getViolationCount();

    LocalDateTime getViolatedAt();

    Integer getSuspendDays();

    LocalDateTime getStartAt();

    LocalDateTime getEndAt();

    LocalDateTime getReleasedAt();
}
