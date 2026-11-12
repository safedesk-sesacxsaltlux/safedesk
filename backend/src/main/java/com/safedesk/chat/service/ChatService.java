package com.safedesk.chat.service;

import com.safedesk.chat.dto.ChatMessageRequestDTO;
import com.safedesk.chat.dto.ChatQueueResponseDTO;
import com.safedesk.chat.entity.Consultation;
import org.springframework.stereotype.Service;
import com.safedesk.chat.repository.MessageRepository;
import com.safedesk.chat.repository.ConsultationRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChatService {

    private final MessageRepository messageRepository;

    private final ConsultationRepository consultationRepository;

    public List<ChatQueueResponseDTO> getQueueList() {

        List<Consultation> consultations = consultationRepository.findByStatusOrderByQueuedAtAsc("QUEUED");

        List<ChatQueueResponseDTO> queueList = new ArrayList<>();

        for(Consultation consultation : consultations) {
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

    public void sendMessage(ChatMessageRequestDTO message) {

    }

    public ChatService(MessageRepository messageRepository, ConsultationRepository consultationRepository) {
        this.messageRepository = messageRepository;
        this.consultationRepository = consultationRepository;
    }
}
