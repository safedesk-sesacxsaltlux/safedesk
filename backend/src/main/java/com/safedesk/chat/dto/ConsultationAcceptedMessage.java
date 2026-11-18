package com.safedesk.chat.dto;

import java.time.LocalDateTime;

/**
 * WebSocket /topic/chats/{consultationId} 로 발행하는 수락 알림 (contract.md WebSocket 「CONSULTATION_ACCEPTED」).
 * 고객 대기 화면(SCR-006)이 이걸 받으면 채팅 화면으로 넘어간다.
 *
 * { "type": "CONSULTATION_ACCEPTED", "consultationId": 101, "agentName": "김상담", "acceptedAt": "2026-11-11T13:00:00" }
 */
public class ConsultationAcceptedMessage {

    private final String type = "CONSULTATION_ACCEPTED";
    private long consultationId;
    private String agentName;
    private LocalDateTime acceptedAt;

    public static ConsultationAcceptedMessage of(long consultationId, String agentName, LocalDateTime acceptedAt) {
        ConsultationAcceptedMessage dto = new ConsultationAcceptedMessage();
        dto.consultationId = consultationId;
        dto.agentName = agentName;
        dto.acceptedAt = acceptedAt;
        return dto;
    }

    public String getType() {
        return type;
    }

    public long getConsultationId() {
        return consultationId;
    }

    public String getAgentName() {
        return agentName;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }
}
