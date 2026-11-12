package com.safedesk.chat.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name="consultation")
public class Consultation {

    @Id
    @Column(name="consultation_id")
    private long consultationId;

    @Column(name="status", nullable = false, length = 20)
    private String status;

    @Column(name="inquiry_category", nullable = true, length = 50)
    private String inquiryCategory;

    @Column(name="inquiry_summary", nullable = true, length = 100)
    private String inquirySummary;

    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name="queued_at", nullable = true)
    private LocalDateTime queuedAt;

    @Column(name="accepted_at", nullable = true)
    private LocalDateTime acceptedAt;

    @Column(name="closed_at", nullable = true)
    private LocalDateTime closedAt;

    @Column(name="closed_by", nullable = true, length = 20)
    private String closedBy;

    @Column(name="close_reason", nullable = true, length = 20)
    private String closeReason;

    @Column(name="process_summary", nullable = true, columnDefinition = "TEXT")
    private String processSummary;

    @Column(name="result", nullable = true, length = 20)
    private String result;

    @Column(name="policy_id", nullable = true)
    private Long policyId;

    @Column(name="customer_id", nullable = false)
    private long customerId;

    @Column(name="agent_id", nullable = true)
    private Long agentId;

    public long getConsultationId() {
        return consultationId;
    }

    public String getStatus() {
        return status;
    }

    public String getInquiryCategory() {
        return inquiryCategory;
    }

    public String getInquirySummary() {
        return inquirySummary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getQueuedAt() {
        return queuedAt;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public String getClosedBy() {
        return closedBy;
    }

    public String getCloseReason() {
        return closeReason;
    }

    public String getProcessSummary() {
        return processSummary;
    }

    public String getResult() {
        return result;
    }

    public Long getPolicyId() {
        return policyId;
    }

    public long getCustomerId() {
        return customerId;
    }

    public Long getAgentId() {
        return agentId;
    }
}
