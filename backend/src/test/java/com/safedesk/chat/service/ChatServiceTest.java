
package com.safedesk.chat.service;

import com.safedesk.chat.dto.ChatCloseResponseDTO;
import com.safedesk.chat.entity.CloseReason;
import com.safedesk.chat.entity.ClosedBy;
import com.safedesk.chat.entity.Consultation;
import com.safedesk.chat.repository.ConsultationRepository;
import com.safedesk.chat.repository.MessageRepository;
import com.safedesk.common.event.ConsultationClosedEvent;
import org.junit.jupiter.api.Test;
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
    void 담당_상담원이_상담을_정상_종료한다() {

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
            eventPublisher
        );

        // 2. 실행
        ChatCloseResponseDTO result = chatService.closeConsultation(1L, 10L);

        // 3. 확인
        assertThat(result.getConsultationStatus()).isEqualTo("CLOSED");

        verify(consultation).close(ClosedBy.AGENT, CloseReason.NORMAL);
        verify(eventPublisher).publishEvent(any(ConsultationClosedEvent.class));
    }

    @Test
    void 다른_상담원은_상담을_종료할_수_없다() {

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
            eventPublisher
        );

        assertThatThrownBy(() -> chatService.closeConsultation(1L, 20L))
            .isInstanceOf(BusinessException.class);

        verify(consultation, never()).close(ClosedBy.AGENT, CloseReason.NORMAL);
        verify(eventPublisher, never()).publishEvent(any(ConsultationClosedEvent.class));
    }

    @Test
    void 이미_종료된_상담은_이벤트를_중복_발행하지_않는다() {

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
            eventPublisher
        );

        chatService.closeConsultation(1L, 10L);

        verify(eventPublisher, never()).publishEvent(any(ConsultationClosedEvent.class));
    }
}
