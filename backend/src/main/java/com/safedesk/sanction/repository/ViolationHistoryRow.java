package com.safedesk.sanction.repository;

import java.time.LocalDateTime;

// API-ADM-05 위반 목록 한 행 (violation + consultation + sanction + abuse_log 조인).
public interface ViolationHistoryRow {

    Long getViolationId();

    Long getConsultationId();

    String getClosedBy();

    LocalDateTime getViolatedAt();

    Long getAbusiveMessageCount();

    Integer getAppliedDurationDays();
}
