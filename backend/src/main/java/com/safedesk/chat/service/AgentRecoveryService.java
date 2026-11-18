package com.safedesk.chat.service;

import com.safedesk.chat.dto.AgentRecoveryResponseDTO;
import com.safedesk.chat.entity.Consultation;
import com.safedesk.chat.entity.ConsultationStatus;
import com.safedesk.chat.repository.ConsultationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 상담원 회복 시간 (REQ-033, policy.md 「상담원 회복 시간」 기본 10분).
 * 폭언으로 종료(TERMINATED)된 상담의 상담원은 종료 후 10분간 새 상담을 수락할 수 없다.
 * 수락(API-002)의 AGENT_RECOVERING 검사와 같은 기준을 쓴다.
 */
@Service
public class AgentRecoveryService {

    public static final long RECOVERY_MINUTES = 10;

    private final ConsultationRepository consultationRepository;
    private final Clock clock;

    @Autowired
    public AgentRecoveryService(ConsultationRepository consultationRepository) {
        this(consultationRepository, Clock.systemDefaultZone());
    }

    // 테스트에서 시간을 고정하기 위한 생성자
    AgentRecoveryService(ConsultationRepository consultationRepository, Clock clock) {
        this.consultationRepository = consultationRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AgentRecoveryResponseDTO getRecovery(Long agentId) {
        LocalDateTime startedAt = consultationRepository
            .findFirstByAgentIdAndStatusOrderByClosedAtDesc(agentId, ConsultationStatus.TERMINATED)
            .map(Consultation::getClosedAt)
            .orElse(null);

        Long activeConsultationId = consultationRepository
            .findFirstByAgentIdAndStatusOrderByAcceptedAtDesc(agentId, ConsultationStatus.IN_PROGRESS)
            .map(Consultation::getConsultationId)
            .orElse(null);
        boolean free = activeConsultationId == null;

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime endsAt = startedAt == null ? null : startedAt.plusMinutes(RECOVERY_MINUTES);
        if (endsAt == null || !now.isBefore(endsAt)) {
            return new AgentRecoveryResponseDTO(agentId, false, null, null, 0, free, activeConsultationId);
        }

        long remaining = Duration.between(now, endsAt).toSeconds();
        return new AgentRecoveryResponseDTO(
            agentId, true, startedAt, endsAt, Math.max(remaining, 1), false, activeConsultationId);
    }
}
