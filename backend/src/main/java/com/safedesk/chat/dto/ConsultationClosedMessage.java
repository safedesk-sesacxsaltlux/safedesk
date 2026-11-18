package com.safedesk.chat.dto;

import com.safedesk.chat.entity.Consultation;

import java.time.LocalDateTime;

/**
 * WebSocket /topic/chats/{consultationId} 로 발행하는 정상 종료 알림 (contract.md WebSocket 「CONSULTATION_CLOSED」).
 * 폭언 종료는 이게 아니라 CONSULTATION_TERMINATED (한수민 MOD-03·07).
 *
 * { "type": "CONSULTATION_CLOSED", "consultationId": 101, "status": "CLOSED", "closedBy": "AGENT", "closedAt": "2026-11-11T13:20:00" }
 */
public class ConsultationClosedMessage {

    private final String type = "CONSULTATION_CLOSED";
    private long consultationId;
    private String status;
    private String closedBy;
    private LocalDateTime closedAt;

    public static ConsultationClosedMessage from(Consultation consultation) {
        ConsultationClosedMessage dto = new ConsultationClosedMessage();
        dto.consultationId = consultation.getConsultationId();
        dto.status = consultation.getStatus();
        dto.closedBy = consultation.getClosedBy();
        dto.closedAt = consultation.getClosedAt();
        return dto;
    }

    public String getType() {
        return type;
    }

    public long getConsultationId() {
        return consultationId;
    }

    public String getStatus() {
        return status;
    }

    public String getClosedBy() {
        return closedBy;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }
}
