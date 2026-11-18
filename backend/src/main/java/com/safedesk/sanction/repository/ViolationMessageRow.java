package com.safedesk.sanction.repository;

import java.time.LocalDateTime;

// API-ADM-07 폭언 메시지 한 행 (abuse_log + message + warning 조인).
public interface ViolationMessageRow {

    Long getMessageId();

    LocalDateTime getSentAt();

    String getDetectedBy();

    Long getWarningOrder();

    String getOriginalContent();

    String getMaskedContent();
}
