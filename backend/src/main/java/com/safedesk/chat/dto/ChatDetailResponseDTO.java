package com.safedesk.chat.dto;

import com.safedesk.chat.entity.Consultation;

import java.time.LocalDateTime;

/**
 * API-003 상담 상세 조회 응답.
 * <p>
 * 상담원 채팅 화면의 고객 정보(누적 위반, 이번 상담 경고)와, 재연결 때 상태 확인(contract.md WebSocket 「재연결」)에 쓴다.
 */
public class ChatDetailResponseDTO {

    private long consultationId;
    private long customerId;
    private String customerName;
    private Long agentId;
    private String agentName;
    private String consultationStatus;
    private String inquiryCategory;
    private String inquirySummary;
    private LocalDateTime createdAt;
    private LocalDateTime queuedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime closedAt;
    private String closedBy;
    private String closeReason;
    private long customerViolationCount;   // 고객 누적 위반 (REQ-019)
    private long warningCount;             // 이번 상담 경고 횟수
    private Integer closeThreshold;        // 자동 종료 기준 (이번 상담에 적용된 정책)

    public static ChatDetailResponseDTO of(Consultation c, String customerName, String agentName,
                                           long customerViolationCount, long warningCount, Integer closeThreshold) {
        ChatDetailResponseDTO dto = new ChatDetailResponseDTO();
        dto.consultationId = c.getConsultationId();
        dto.customerId = c.getCustomerId();
        dto.customerName = customerName;
        dto.agentId = c.getAgentId();
        dto.agentName = agentName;
        dto.consultationStatus = c.getStatus();
        dto.inquiryCategory = c.getInquiryCategory();
        dto.inquirySummary = c.getInquirySummary();
        dto.createdAt = c.getCreatedAt();
        dto.queuedAt = c.getQueuedAt();
        dto.acceptedAt = c.getAcceptedAt();
        dto.closedAt = c.getClosedAt();
        dto.closedBy = c.getClosedBy();
        dto.closeReason = c.getCloseReason();
        dto.customerViolationCount = customerViolationCount;
        dto.warningCount = warningCount;
        dto.closeThreshold = closeThreshold;
        return dto;
    }

    public long getConsultationId() { return consultationId; }
    public long getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public Long getAgentId() { return agentId; }
    public String getAgentName() { return agentName; }
    public String getConsultationStatus() { return consultationStatus; }
    public String getInquiryCategory() { return inquiryCategory; }
    public String getInquirySummary() { return inquirySummary; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getQueuedAt() { return queuedAt; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public String getClosedBy() { return closedBy; }
    public String getCloseReason() { return closeReason; }
    public long getCustomerViolationCount() { return customerViolationCount; }
    public long getWarningCount() { return warningCount; }
    public Integer getCloseThreshold() { return closeThreshold; }
}
