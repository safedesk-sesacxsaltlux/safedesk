
package com.safedesk.chat.service;

import com.safedesk.chat.dto.ChatCloseResponseDTO;
import com.safedesk.chat.dto.ChatMessageRequestDTO;
import com.safedesk.chat.dto.ChatMessageResponseDTO;
import com.safedesk.chat.entity.CloseReason;
import com.safedesk.chat.entity.ClosedBy;
import com.safedesk.chat.entity.Consultation;
import com.safedesk.chat.entity.ConsultationStatus;
import com.safedesk.chat.entity.Message;
import com.safedesk.chat.repository.ConsultationRepository;
import com.safedesk.chat.repository.MessageRepository;
import com.safedesk.common.event.ConsultationClosedEvent;
import com.safedesk.moderation.dto.AbuseFilterResult;
import com.safedesk.moderation.service.ModerationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ChatServiceTest {

    @Test
    @DisplayName("담당 상담원이 상담을 정상 종료한다")
    void closeByAssignedAgent() {

        // 1. 준비
        ConsultationRepository consultationRepository = mock(ConsultationRepository.class);
        MessageRepository messageRepository = mock(MessageRepository.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        Consultation consultation = mock(Consultation.class);

        when(consultationRepository.findWithLockByConsultationId(1L))
            .thenReturn(Optional.of(consultation));
        when(consultation.getAgentId()).thenReturn(10L);
        when(consultation.getStatus()).thenReturn("IN_PROGRESS", "CLOSED");
        when(consultation.close(ClosedBy.AGENT, CloseReason.NORMAL)).thenReturn(true);

        ChatService chatService = new ChatService(
            messageRepository,
            messagingTemplate,
            consultationRepository,
            eventPublisher,
            mock(ModerationService.class)
        );

        // 2. 실행
        ChatCloseResponseDTO result = chatService.closeConsultation(1L, 10L);

        // 3. 확인
        assertThat(result.getConsultationStatus()).isEqualTo("CLOSED");

        verify(consultation).close(ClosedBy.AGENT, CloseReason.NORMAL);
        verify(eventPublisher).publishEvent(any(ConsultationClosedEvent.class));
    }

    @Test
    @DisplayName("다른 상담원은 상담을 종료할 수 없다")
    void closeByOtherAgent() {

        ConsultationRepository consultationRepository = mock(ConsultationRepository.class);
        MessageRepository messageRepository = mock(MessageRepository.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        Consultation consultation = mock(Consultation.class);

        when(consultationRepository.findWithLockByConsultationId(1L))
            .thenReturn(Optional.of(consultation));
        when(consultation.getAgentId()).thenReturn(10L);

        ChatService chatService = new ChatService(
            messageRepository,
            messagingTemplate,
            consultationRepository,
            eventPublisher,
            mock(ModerationService.class)
        );

        assertThatThrownBy(() -> chatService.closeConsultation(1L, 20L))
            .isInstanceOf(BusinessException.class);

        verify(consultation, never()).close(ClosedBy.AGENT, CloseReason.NORMAL);
        verify(eventPublisher, never()).publishEvent(any(ConsultationClosedEvent.class));
    }

    @Test
    @DisplayName("이미 종료된 상담은 이벤트를 중복 발행하지 않는다")
    void closeAlreadyClosed() {

        ConsultationRepository consultationRepository = mock(ConsultationRepository.class);
        MessageRepository messageRepository = mock(MessageRepository.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        Consultation consultation = mock(Consultation.class);

        when(consultationRepository.findWithLockByConsultationId(1L))
            .thenReturn(Optional.of(consultation));
        when(consultation.getAgentId()).thenReturn(10L);
        when(consultation.getStatus()).thenReturn("CLOSED");
        when(consultation.close(ClosedBy.AGENT, CloseReason.NORMAL)).thenReturn(false);

        ChatService chatService = new ChatService(
            messageRepository,
            messagingTemplate,
            consultationRepository,
            eventPublisher,
            mock(ModerationService.class)
        );

        chatService.closeConsultation(1L, 10L);

        verify(eventPublisher, never()).publishEvent(any(ConsultationClosedEvent.class));
    }

    // ===== 폭언 필터 연결 (MOD-01, 이슈 #25) =====

    @Test
    @DisplayName("상담 중 고객 폭언은 마스킹해서 저장하고 원문은 따로 저장한다")
    void maskAbusiveMessage() {

        // 1. 준비
        ConsultationRepository consultationRepository = mock(ConsultationRepository.class);
        MessageRepository messageRepository = mock(MessageRepository.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        ModerationService moderationService = mock(ModerationService.class);
        Consultation consultation = mock(Consultation.class);

        when(consultationRepository.findById(1L)).thenReturn(Optional.of(consultation));
        when(consultation.getStatus()).thenReturn(ConsultationStatus.IN_PROGRESS);
        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(moderationService.processMessage("병신같네"))
            .thenReturn(AbuseFilterResult.abusive("***같네"));

        ChatMessageRequestDTO request = new ChatMessageRequestDTO();
        request.setSenderType("CUSTOMER");
        request.setSenderId(100L);
        request.setMessageContent("병신같네");

        ChatService chatService = new ChatService(
            messageRepository,
            messagingTemplate,
            consultationRepository,
            eventPublisher,
            moderationService
        );

        // 2. 실행
        chatService.sendMessage(1L, request);

        // 3. 확인 - DB 에는 마스킹본 저장
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getContent()).isEqualTo("***같네");
        assertThat(messageCaptor.getValue().getAbuseStatus()).isEqualTo("ABUSIVE");

        // 원문은 abuse_log 에 따로 저장
        verify(moderationService).saveOriginal(anyLong(), eq(1L), eq("병신같네"));

        // 상담원에게 발행되는 메시지에 원문이 없음
        ArgumentCaptor<ChatMessageResponseDTO> payloadCaptor =
            ArgumentCaptor.forClass(ChatMessageResponseDTO.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/chats/1"), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue().getContent()).isEqualTo("***같네");
    }

    @Test
    @DisplayName("AI 상담 중 메시지는 검사하지 않는다")
    void skipAiChatMessage() {

        // 1. 준비
        ConsultationRepository consultationRepository = mock(ConsultationRepository.class);
        MessageRepository messageRepository = mock(MessageRepository.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        ModerationService moderationService = mock(ModerationService.class);
        Consultation consultation = mock(Consultation.class);

        when(consultationRepository.findById(1L)).thenReturn(Optional.of(consultation));
        when(consultation.getStatus()).thenReturn(ConsultationStatus.AI_CHAT);
        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChatMessageRequestDTO request = new ChatMessageRequestDTO();
        request.setSenderType("CUSTOMER");
        request.setSenderId(100L);
        request.setMessageContent("병신같네");

        ChatService chatService = new ChatService(
            messageRepository,
            messagingTemplate,
            consultationRepository,
            eventPublisher,
            moderationService
        );

        // 2. 실행
        chatService.sendMessage(1L, request);

        // 3. 확인 - 필터를 거치지 않고 원문 그대로, abuseStatus 는 NULL (체크리스트 E11)
        verify(moderationService, never()).processMessage(any());
        verify(moderationService, never()).saveOriginal(anyLong(), anyLong(), any());

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getContent()).isEqualTo("병신같네");
        assertThat(messageCaptor.getValue().getAbuseStatus()).isNull();
    }

    @Test
    @DisplayName("상담원 메시지는 검사하지 않는다")
    void skipAgentMessage() {

        // 1. 준비
        ConsultationRepository consultationRepository = mock(ConsultationRepository.class);
        MessageRepository messageRepository = mock(MessageRepository.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        ModerationService moderationService = mock(ModerationService.class);
        Consultation consultation = mock(Consultation.class);

        when(consultationRepository.findById(1L)).thenReturn(Optional.of(consultation));
        when(consultation.getStatus()).thenReturn(ConsultationStatus.IN_PROGRESS);
        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChatMessageRequestDTO request = new ChatMessageRequestDTO();
        request.setSenderType("AGENT");
        request.setSenderId(10L);
        request.setMessageContent("확인 후 안내드리겠습니다.");

        ChatService chatService = new ChatService(
            messageRepository,
            messagingTemplate,
            consultationRepository,
            eventPublisher,
            moderationService
        );

        // 2. 실행
        chatService.sendMessage(1L, request);

        // 3. 확인
        verify(moderationService, never()).processMessage(any());

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getContent()).isEqualTo("확인 후 안내드리겠습니다.");
        assertThat(messageCaptor.getValue().getAbuseStatus()).isNull();
    }
}
