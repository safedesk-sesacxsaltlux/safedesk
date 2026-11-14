package com.safedesk.chat.dto;

import java.time.LocalDateTime;

    public class ChatCloseResponseDTO {

        private Long consultationId;
        private String consultationStatus;
        private Long agentId;
        private LocalDateTime closedAt;
        private String closedBy;
        private String closeReason;

        public Long getConsultationId() {
            return consultationId;
        }

        public String getConsultationStatus() {
            return consultationStatus;
        }

        public Long getAgentId() {
            return agentId;
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

        public void setConsultationId(Long consultationId) {
            this.consultationId = consultationId;
        }

        public void setConsultationStatus(String consultationStatus) {
            this.consultationStatus = consultationStatus;
        }

        public void setAgentId(Long agentId) {
            this.agentId = agentId;
        }

        public void setClosedAt(LocalDateTime closedAt) {
            this.closedAt = closedAt;
        }

        public void setClosedBy(String closedBy) {
            this.closedBy = closedBy;
        }

        public void setCloseReason(String closeReason) {
            this.closeReason = closeReason;
        }
}


