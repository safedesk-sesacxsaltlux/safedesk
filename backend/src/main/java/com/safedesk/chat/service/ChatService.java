package com.safedesk.chat.service;

import com.safedesk.chat.dto.ChatMessageRequestDTO;
import com.safedesk.chat.dto.ChatQueueResponseDTO;
import com.safedesk.chat.entity.Consultation;
import org.springframework.stereotype.Service;
import com.safedesk.chat.repository.MessageRepository;
import com.safedesk.chat.repository.ConsultationRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.safedesk.chat.entity.Message;

@Service
public class ChatService {

    private final MessageRepository messageRepository;

    private final SimpMessagingTemplate simpMessagingTemplate;

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

    public ChatService(MessageRepository messageRepository, SimpMessagingTemplate simpMessagingTemplate, ConsultationRepository consultationRepository) {
        this.messageRepository = messageRepository;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.consultationRepository = consultationRepository;
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
}
