
package com.safedesk.chat.service;

import com.safedesk.chat.dto.*;
import com.safedesk.chat.entity.CloseReason;
import com.safedesk.chat.entity.ClosedBy;
import com.safedesk.chat.entity.Consultation;
import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.moderation.dto.AbuseFilterResult;
import com.safedesk.moderation.service.ModerationService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import com.safedesk.chat.repository.MessageRepository;
import com.safedesk.chat.repository.ConsultationRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.safedesk.common.event.ConsultationClosedEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.safedesk.chat.entity.Message;
import com.safedesk.chat.entity.ConsultationStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ChatService {

    private final MessageRepository messageRepository;

    private final SimpMessagingTemplate simpMessagingTemplate;

    private final ConsultationRepository consultationRepository;

    private final ApplicationEventPublisher eventPublisher;

    private final ModerationService moderationService;

    public List<ChatQueueResponseDTO> getQueueList() {

        List<Consultation> consultations = consultationRepository.findByStatusOrderByQueuedAtAsc(ConsultationStatus.QUEUED);
        List<ChatQueueResponseDTO> queueList = new ArrayList<>();

        for (Consultation consultation : consultations) {
            ChatQueueResponseDTO chatQueueResponseDTO = new ChatQueueResponseDTO();

            chatQueueResponseDTO.setConsultationId(consultation.getConsultationId());
            chatQueueResponseDTO.setCustomerId(String.valueOf(consultation.getCustomerId()));
            chatQueueResponseDTO.setConsultationStatus(consultation.getStatus());
            chatQueueResponseDTO.setQueuedAt(consultation.getQueuedAt());

            if (consultation.getQueuedAt() == null) {
                chatQueueResponseDTO.setWaitingTime(0);
            } else {
                chatQueueResponseDTO.setWaitingTime(
                    Duration.between(consultation.getQueuedAt(), LocalDateTime.now()).getSeconds());
            }

            queueList.add(chatQueueResponseDTO);

        }

        return queueList;
    }

    public ChatService(
        MessageRepository messageRepository, SimpMessagingTemplate simpMessagingTemplate,
        ConsultationRepository consultationRepository, ApplicationEventPublisher eventPublisher,
        ModerationService moderationService) {
        this.messageRepository = messageRepository;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.consultationRepository = consultationRepository;
        this.eventPublisher = eventPublisher;
        this.moderationService = moderationService;
    }

    @Transactional
    public void sendMessage(long consultationId, ChatMessageRequestDTO message) {

        Consultation consultation = consultationRepository.findById(consultationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_NOT_FOUND));

        boolean shouldModerate = ConsultationStatus.IN_PROGRESS.equals(consultation.getStatus())
            && "CUSTOMER".equals(message.getSenderType());
        // 로그인(#24) 전이라 임시로 DTO 의 senderType 으로 판단
        // 로그인이 들어오면 로그인 토큰 기준으로 바꾸기
        // AI 상담 중 메시지·상담원 메시지는 검사하지 않는다 (정책 4장, 체크리스트 E11)

        Message chatMessage = new Message();

        chatMessage.setCreatedAt(LocalDateTime.now());

        String original = message.getMessageContent();
        AbuseFilterResult result = null;

        if (shouldModerate) {
            result = moderationService.processMessage(original);
            chatMessage.setContent(result.maskedContent());
            chatMessage.setAbuseStatus(result.abuseStatus().name());
        } else {
            chatMessage.setContent(original);
        }
        chatMessage.setSenderType(message.getSenderType());
        chatMessage.setSenderId(message.getSenderId());
        chatMessage.setConsultationId(consultationId);

        Message saved = messageRepository.save(chatMessage);

        if (shouldModerate && result.detected()) {
            moderationService.saveOriginal(saved.getMessageId(), consultationId, original);
        }

        sendAfterCommit("/topic/chats/" + consultationId,
            ChatMessageResponseDTO.from(saved)
        );
    }

    // 커밋된 뒤에만 발행한다. 저장 중 예외로 롤백되면 발행하지 않는다 (원문 노출 없이 전송 보류, moderation.md)
    private void sendAfterCommit(String destination, Object payload) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    simpMessagingTemplate.convertAndSend(destination, payload);
                }
            });
        } else {
            simpMessagingTemplate.convertAndSend(destination, payload);   // 트랜잭션 없는 단위 테스트용
        }
    }

    @Transactional
    public ChatAcceptResponseDTO acceptConsultation(long consultationId, Long agentId) {
        Consultation consultation = consultationRepository
            .findWithLockByConsultationId(consultationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_NOT_FOUND));

        if (consultationRepository.existsByAgentIdAndStatus(agentId, ConsultationStatus.IN_PROGRESS)) {
            throw new BusinessException(ErrorCode.AGENT_BUSY);
        }

        Consultation lastTerminated = consultationRepository.findFirstByAgentIdAndStatusOrderByClosedAtDesc(agentId, ConsultationStatus.TERMINATED)
            .orElse(null);

        if (lastTerminated != null && lastTerminated.getClosedAt() != null) {
            long elapsedMinutes = Duration.between(lastTerminated.getClosedAt(), LocalDateTime.now()).toMinutes();

            if (elapsedMinutes < 10) {
                throw new BusinessException(ErrorCode.AGENT_RECOVERING);
            }
        }

        if (consultationRepository.accept(consultationId, agentId, LocalDateTime.now()) == 0) {
            throw new BusinessException(ErrorCode.CHAT_ALREADY_ACCEPTED);
        }

        consultation = consultationRepository.findById(consultationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_NOT_FOUND));

        ChatAcceptResponseDTO response = new ChatAcceptResponseDTO();
        response.setConsultationId(consultation.getConsultationId());
        response.setCustomerId(consultation.getCustomerId());
        response.setAgentId(consultation.getAgentId());
        response.setConsultationStatus(consultation.getStatus());
        response.setAcceptedAt(consultation.getAcceptedAt());

        return response;
    }

    @Transactional
    public ChatCloseResponseDTO closeConsultation(long consultationId, Long agentId) {
        Consultation consultation = consultationRepository
            .findWithLockByConsultationId(consultationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_NOT_FOUND));

        if (!agentId.equals(consultation.getAgentId())) {
            throw new BusinessException(ErrorCode.CHAT_ACCESS_DENIED);
        }

        if (!ConsultationStatus.IN_PROGRESS.equals(consultation.getStatus())
            && !ConsultationStatus.CLOSED.equals(consultation.getStatus())
            && !ConsultationStatus.TERMINATED.equals(consultation.getStatus())) {
            throw new BusinessException(ErrorCode.CHAT_NOT_ACTIVE);
        }

        if (consultation.close(ClosedBy.AGENT, CloseReason.NORMAL)) {
            eventPublisher.publishEvent(
                new ConsultationClosedEvent(consultationId)
            );
        }

        ChatCloseResponseDTO response = new ChatCloseResponseDTO();

        response.setConsultationId(consultation.getConsultationId());
        response.setConsultationStatus(consultation.getStatus());
        response.setAgentId(consultation.getAgentId());
        response.setClosedAt(consultation.getClosedAt());
        response.setClosedBy(consultation.getClosedBy());
        response.setCloseReason(consultation.getCloseReason());

        return response;
    }

}
