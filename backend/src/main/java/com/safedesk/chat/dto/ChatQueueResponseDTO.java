package com.safedesk.chat.dto;

import java.time.LocalDateTime;

public class ChatQueueResponseDTO {

    private long consultationId;    // 상담 ID
    private String customerId;    // 고객 ID
    private String consultationStatus;    // 상담 상태
    private LocalDateTime queuedAt; // 대기열 진입 시각
    private long waitingTime;   // 대기 시간

    public long getConsultationId() {
        return consultationId;
    }

    public void setConsultationId(long consultationId) {
        this.consultationId = consultationId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getConsultationStatus() {
        return consultationStatus;
    }

    public void setConsultationStatus(String consultationStatus) {
        this.consultationStatus = consultationStatus;
    }

    public LocalDateTime getQueuedAt() {
        return queuedAt;
    }

    public void setQueuedAt(LocalDateTime queuedAt) {
        this.queuedAt = queuedAt;
    }

    public long getWaitingTime() {
        return waitingTime;
    }

    public void setWaitingTime(long waitingTime) {
        this.waitingTime = waitingTime;
    }
}
