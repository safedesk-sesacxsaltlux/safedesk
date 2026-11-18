package com.safedesk.chat.service;

import com.safedesk.chat.dto.ChatDetailResponseDTO;
import com.safedesk.chat.dto.ChatHistoryResponseDTO;
import com.safedesk.chat.entity.Consultation;
import com.safedesk.chat.entity.Message;
import com.safedesk.chat.repository.ConsultationRepository;
import com.safedesk.chat.repository.MessageRepository;
import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.member.entity.Member;
import com.safedesk.member.repository.MemberRepository;
import com.safedesk.moderation.repository.WarningRepository;
import com.safedesk.sanction.entity.SanctionPolicy;
import com.safedesk.sanction.repository.SanctionPolicyRepository;
import com.safedesk.sanction.repository.ViolationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 상담 조회 API (API-003 상세, API-004 메시지, API-007 AI 응대 내역).
 * 조회만 하고 상태·메시지를 바꾸지 않는다.
 */
@Service
public class ChatHistoryService {

    private final MessageRepository messageRepository;
    private final ConsultationRepository consultationRepository;
    private final MemberRepository memberRepository;
    private final ViolationRepository violationRepository;
    private final WarningRepository warningRepository;
    private final SanctionPolicyRepository sanctionPolicyRepository;

    public ChatHistoryService(
        MessageRepository messageRepository,
        ConsultationRepository consultationRepository,
        MemberRepository memberRepository,
        ViolationRepository violationRepository,
        WarningRepository warningRepository,
        SanctionPolicyRepository sanctionPolicyRepository
    ) {
        this.messageRepository = messageRepository;
        this.consultationRepository = consultationRepository;
        this.memberRepository = memberRepository;
        this.violationRepository = violationRepository;
        this.warningRepository = warningRepository;
        this.sanctionPolicyRepository = sanctionPolicyRepository;
    }

    /**
     * API-003 상담 상세. 해당 상담의 고객이나 수락한 상담원만 볼 수 있다 (NFR-004).
     */
    @Transactional(readOnly = true)
    public ChatDetailResponseDTO getChatDetail(long consultationId, Long memberId) {
        Consultation consultation = findParticipantConsultation(consultationId, memberId);

        String customerName = nameOf(consultation.getCustomerId());
        String agentName = consultation.getAgentId() == null ? null : nameOf(consultation.getAgentId());
        long violationCount = violationRepository.countByCustomerId(consultation.getCustomerId());
        long warningCount = warningRepository.countByConsultationId(consultationId);
        SanctionPolicy policy = sanctionPolicyRepository.findByIdOrLatest(consultation.getPolicyId());
        Integer closeThreshold = policy == null ? null : policy.getCloseThreshold();

        return ChatDetailResponseDTO.of(consultation, customerName, agentName, violationCount, warningCount, closeThreshold);
    }

    /**
     * API-004 상담 메시지 내역. 고객·수락한 상담원 모두 쓴다.
     * 재연결 때 끊긴 동안 온 메시지를 다시 불러오는 용도 (NFR-008, 체크리스트 E6).
     * content 는 저장된 마스킹본이다. 원문(abuse_log)은 포함하지 않는다 (NFR-005).
     */
    @Transactional(readOnly = true)
    public List<ChatHistoryResponseDTO> getChatHistory(long consultationId, Long memberId) {
        findParticipantConsultation(consultationId, memberId);

        return messageRepository
            .findByConsultationIdOrderByCreatedAtAscMessageIdAsc(consultationId)
            .stream()
            .map(ChatHistoryResponseDTO::from)
            .toList();
    }

    /**
     * API-007 AI 응대 내역 (REQ-012). 수락한 상담원이 상담원 연결 전 고객·AI 대화를 본다.
     * 수락 시각(accepted_at) 전에 저장된 고객·AI 메시지만 준다.
     * queued_at 은 DB 에서 초 단위로 잘려 같은 초의 AI 답변이 빠질 수 있어 기준으로 쓰지 않는다.
     * (수락 전에는 고객이 실시간 메시지를 보낼 수 없고, 수락 뒤에는 AI 가 답하지 않는다)
     */
    @Transactional(readOnly = true)
    public List<ChatHistoryResponseDTO> getAiMessages(long consultationId, Long agentId) {
        Consultation consultation = consultationRepository.findById(consultationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_NOT_FOUND));

        if (!Objects.equals(agentId, consultation.getAgentId())) {
            throw new BusinessException(ErrorCode.CHAT_ACCESS_DENIED);
        }

        LocalDateTime handoffAt = consultation.getAcceptedAt();
        if (handoffAt == null) {
            return List.of();
        }

        return messageRepository
            .findByConsultationIdOrderByCreatedAtAscMessageIdAsc(consultationId)
            .stream()
            .filter(m -> m.getCreatedAt() != null && m.getCreatedAt().isBefore(handoffAt))
            .filter(ChatHistoryService::isAiPhaseMessage)
            .map(ChatHistoryResponseDTO::from)
            .toList();
    }

    private static boolean isAiPhaseMessage(Message message) {
        return "CUSTOMER".equals(message.getSenderType()) || "AI".equals(message.getSenderType());
    }

    private Consultation findParticipantConsultation(long consultationId, Long memberId) {
        Consultation consultation = consultationRepository.findById(consultationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_NOT_FOUND));

        boolean isCustomer = memberId != null && memberId.equals(consultation.getCustomerId());
        boolean isAssignedAgent = memberId != null && memberId.equals(consultation.getAgentId());
        if (!isCustomer && !isAssignedAgent) {
            throw new BusinessException(ErrorCode.CHAT_ACCESS_DENIED);
        }
        return consultation;
    }

    private String nameOf(Long memberId) {
        return memberRepository.findById(memberId).map(Member::getName).orElse(null);
    }
}
