
package com.safedesk.chat.entity;

import com.safedesk.common.exception.ErrorCode;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import com.safedesk.common.exception.BusinessException;

@Entity
@Table(name="consultation")
public class Consultation {

    @Id
    @Column(name = "consultation_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long consultationId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "inquiry_category", nullable = true, length = 50)
    private String inquiryCategory;

    @Column(name = "inquiry_summary", nullable = true, length = 100)
    private String inquirySummary;

    @Column(name = "faq_id", nullable = true)
    private Long faqId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "queued_at", nullable = true)
    private LocalDateTime queuedAt;

    @Column(name = "accepted_at", nullable = true)
    private LocalDateTime acceptedAt;

    @Column(name = "closed_at", nullable = true)
    private LocalDateTime closedAt;

    @Column(name = "closed_by", nullable = true, length = 20)
    private String closedBy;

    @Column(name = "close_reason", nullable = true, length = 20)
    private String closeReason;

    @Column(name = "process_summary", nullable = true, columnDefinition = "TEXT")
    private String processSummary;

    @Column(name = "result", nullable = true, length = 20)
    private String result;

    @Column(name = "policy_id", nullable = true)
    private Long policyId;

    @Column(name = "customer_id", nullable = false)
    private long customerId;

    @Column(name = "agent_id", nullable = true)
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

    public Long getFaqId() {
        return faqId;
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

    // CUS-02 6단계: 처음 매칭된 FAQ만 남긴다. 이미 있으면 무시한다.
    // 문의 유형도 같은 FAQ 기준으로 같이 저장해서 둘이 어긋나지 않게 한다.
    public void recordFirstFaqMatch(Long faqId, String category) {
        if(this.faqId != null) {
            return;
        }
        this.faqId = faqId;
        this.inquiryCategory = category;
    }

    // AI 상담을 상담원에게 넘기기 위해 대기열에 등록하는 메서드
    public void handoff() {
        if (!ConsultationStatus.AI_CHAT.equals(this.status)) {
            throw new BusinessException(ErrorCode.CHAT_ALREADY_ACTIVE);
        }
        this.status = ConsultationStatus.QUEUED;
        this.queuedAt = LocalDateTime.now();
    }

    public void accept(Long agentId) {
        if (!ConsultationStatus.QUEUED.equals(this.status)) {
            throw new BusinessException(ErrorCode.CHAT_ALREADY_ACCEPTED);
        }
        this.status = ConsultationStatus.IN_PROGRESS;
        this.agentId = agentId;
        this.acceptedAt = LocalDateTime.now();
    }

    public boolean close(String closedBy, String closeReason) {
        if (ConsultationStatus.CLOSED.equals(this.status) || ConsultationStatus.TERMINATED.equals(this.status)) {
            return false;
        }
        this.status = ConsultationStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
        this.closedBy = closedBy;
        this.closeReason = closeReason;
        return true;
    }

    public boolean terminate(String closedBy, String closeReason) {
        if (ConsultationStatus.CLOSED.equals(this.status) || ConsultationStatus.TERMINATED.equals(this.status)) {
            return false;
        }

        if (!ConsultationStatus.IN_PROGRESS.equals(this.status)) {
            throw new BusinessException(ErrorCode.CHAT_NOT_ACTIVE);
        }

        this.status = ConsultationStatus.TERMINATED;
        this.closedAt = LocalDateTime.now();
        this.closedBy = closedBy;
        this.closeReason = closeReason;
        return true;
    }

    public static Consultation start(Long customerId, Long policyId) {
        Consultation consultation = new Consultation();

        consultation.customerId = customerId;
        consultation.policyId = policyId;
        consultation.status = ConsultationStatus.AI_CHAT;
        consultation.createdAt = LocalDateTime.now();
        return consultation;
    }
}
