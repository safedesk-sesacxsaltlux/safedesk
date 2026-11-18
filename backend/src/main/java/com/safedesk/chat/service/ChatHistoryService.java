
package com.safedesk.chat.service;

import com.safedesk.chat.dto.ChatHistoryResponseDTO;
import com.safedesk.chat.entity.Consultation;
import com.safedesk.chat.repository.ConsultationRepository;
import com.safedesk.chat.repository.MessageRepository;
import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatHistoryService {

    private final MessageRepository messageRepository;
    private final ConsultationRepository consultationRepository;

    public ChatHistoryService(
        MessageRepository messageRepository,
        ConsultationRepository consultationRepository
    ) {
        this.messageRepository = messageRepository;
        this.consultationRepository = consultationRepository;
    }

    @Transactional(readOnly = true)
    public List<ChatHistoryResponseDTO> getChatHistory(
        long consultationId, Long agentId
    ) {
        Consultation consultation = consultationRepository.findById(consultationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_NOT_FOUND));

        if (!agentId.equals(consultation.getAgentId())) {
            throw new BusinessException(ErrorCode.CHAT_ACCESS_DENIED);
        }

        return messageRepository
            .findByConsultationIdOrderByCreatedAtAsc(consultationId)
            .stream()
            .map(ChatHistoryResponseDTO::from)
            .toList();
    }
}
