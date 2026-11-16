
package com.safedesk.chat.service;

import com.safedesk.chat.dto.ChatMessageRequestDTO;
import com.safedesk.chat.dto.ChatQueueResponseDTO;
import com.safedesk.chat.entity.CloseReason;
import com.safedesk.chat.entity.ClosedBy;
import com.safedesk.chat.entity.Consultation;
import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
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
import com.safedesk.chat.dto.ChatAcceptResponseDTO;
import com.safedesk.chat.dto.ChatCloseResponseDTO;
import com.safedesk.chat.entity.ConsultationStatus;

@Service
public class ChatService {

    private final MessageRepository messageRepository;

    private final SimpMessagingTemplate simpMessagingTemplate;

    private final ConsultationRepository consultationRepository;

    private final ApplicationEventPublisher eventPublisher;

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

    public ChatService(MessageRepository messageRepository, SimpMessagingTemplate simpMessagingTemplate, ConsultationRepository consultationRepository, ApplicationEventPublisher eventPublisher) {
        this.messageRepository = messageRepository;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.consultationRepository = consultationRepository;
        this.eventPublisher = eventPublisher;
    }

    public void sendMessage(long consultationId, ChatMessageRequestDTO message) {

        Message chatMessage = new Message();

        chatMessage.setCreatedAt(LocalDateTime.now());

        chatMessage.setContent(message.getMessageContent());
        chatMessage.setSenderType(message.getSenderType());
        chatMessage.setSenderId(message.getSenderId());
        chatMessage.setConsultationId(consultationId);

        messageRepository.save(chatMessage);

        simpMessagingTemplate.convertAndSend(
            "/topic/chats/" + consultationId,
            message
        );
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
