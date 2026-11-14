package com.safedesk.chat.dto;

import java.time.LocalDateTime;

public class ChatAcceptResponseDTO {

    private Long consultationId;
    private Long customerId;
    private Long agentId;
    private String consultationStatus;
    private LocalDateTime acceptedAt;

    public Long getConsultationId() {
        return consultationId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Long getAgentId() {
        return agentId;
    }

    public String getConsultationStatus() {
        return consultationStatus;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setConsultationId(Long consultationId) {
        this.consultationId = consultationId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public void setConsultationStatus(String consultationStatus) {
        this.consultationStatus = consultationStatus;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }
}
