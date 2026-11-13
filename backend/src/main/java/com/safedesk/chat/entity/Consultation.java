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

    // AI 상담을 상담원에게 넘기기 위해 대기열에 등록하는 메서드
    public void handoff() {
        if(!"AI_CHAT".equals(this.status)) {
            throw new IllegalStateException("AI 상담 중에만 상담원 연결을 요청할 수 있습니다.");
        }
        this.status = "QUEUED";
        this.queuedAt = LocalDateTime.now();
    }

    public void accept(Long agentId) {
        if(!"QUEUED".equals(this.status)) {
            throw new IllegalStateException("이미 다른 상담원이 수락한 상담이거나 대기 중인 상담이 아닙니다.");
        }
        this.status = "IN_PROGRESS";
        this.agentId = agentId;
        this.acceptedAt = LocalDateTime.now();
    }

    public void close(String closedBy, String closeReason) {
        if("CLOSED".equals(this.status) || "TERMINATED".equals(this.status)) {
            throw new IllegalStateException("이미 종료된 상담입니다.");
        }
        this.status = "CLOSED";
        this.closedAt = LocalDateTime.now();
        this.closedBy = closedBy;
        this.closeReason = closeReason;
    }

    public void terminate(String closedBy, String closeReason) {
        if(!"IN_PROGRESS".equals(this.status)) {
            throw new IllegalStateException("진행 중인 상담만 강제 종료할 수 있습니다.");
        }
        this.status = "TERMINATED";
        this.closedAt = LocalDateTime.now();
        this.closedBy = closedBy;
        this.closeReason = closeReason;
    }
}
