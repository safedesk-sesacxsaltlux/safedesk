
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
import com.safedesk.member.repository.MemberRepository;
import com.safedesk.common.event.ConsultationClosedEvent;
import com.safedesk.moderation.dto.AbuseFilterResult;
import com.safedesk.moderation.dto.ConsultationTerminatedMessage;
import com.safedesk.moderation.dto.TerminationResult;
import com.safedesk.moderation.dto.WarningIssuedMessage;
import com.safedesk.moderation.dto.WarningResult;
import com.safedesk.moderation.entity.AbuseLog;
import com.safedesk.moderation.service.ModerationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.time.LocalDateTime;
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
            mock(ModerationService.class),
            mock(MemberRepository.class)
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
            mock(ModerationService.class),
            mock(MemberRepository.class)
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
            mock(ModerationService.class),
            mock(MemberRepository.class)
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

        when(consultationRepository.findWithLockByConsultationId(1L)).thenReturn(Optional.of(consultation));
        when(consultation.getStatus()).thenReturn(ConsultationStatus.IN_PROGRESS);
        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(moderationService.processMessage("병신같네"))
            .thenReturn(AbuseFilterResult.abusive("***같네"));
        // saveOriginal 이 돌려준 abuseLogId 로 경고를 건다. mock 기본값(null)이면 NPE 가 나므로 준비해 둔다
        AbuseLog abuseLog = mock(AbuseLog.class);
        when(abuseLog.getAbuseLogId()).thenReturn(10L);
        when(moderationService.saveOriginal(anyLong(), anyLong(), any())).thenReturn(abuseLog);
        // 이 테스트는 마스킹·원문 저장만 본다. 경고는 아래 경고 테스트에서 확인
        when(moderationService.applyAutoWarning(1L, 10L)).thenReturn(Optional.empty());

        ChatMessageRequestDTO request = new ChatMessageRequestDTO();
        request.setSenderType("CUSTOMER");
        request.setSenderId(100L);
        request.setMessageContent("병신같네");

        ChatService chatService = new ChatService(
            messageRepository,
            messagingTemplate,
            consultationRepository,
            eventPublisher,
            moderationService,
            mock(MemberRepository.class)
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

        // 상담원에게 발행되는 메시지에 원문이 없음 (경고가 없으니 발행은 MESSAGE 1번)
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

        when(consultationRepository.findWithLockByConsultationId(1L)).thenReturn(Optional.of(consultation));
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
            moderationService,
            mock(MemberRepository.class)
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

        when(consultationRepository.findWithLockByConsultationId(1L)).thenReturn(Optional.of(consultation));
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
            moderationService,
            mock(MemberRepository.class)
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

    // ===== 자동 경고 (MOD-02) =====

    @Test
    @DisplayName("첫 폭언이면 마스킹된 메시지 다음에 경고 알림을 발행한다")
    void publishWarningAfterMessage() {

        // 1. 준비
        ConsultationRepository consultationRepository = mock(ConsultationRepository.class);
        MessageRepository messageRepository = mock(MessageRepository.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        ModerationService moderationService = mock(ModerationService.class);
        ChatService chatService = abusiveMessageSetUp(
            consultationRepository, messageRepository, messagingTemplate, moderationService);

        // 경고 1회, 종료 기준 2회 → 아직 종료 아님
        when(moderationService.applyAutoWarning(1L, 10L))
            .thenReturn(Optional.of(new WarningResult(1L, 501L, 1, 2, false)));

        // 2. 실행
        chatService.sendMessage(1L, abusiveRequest());

        // 3. 확인 - 발행 2번: 마스킹된 메시지 먼저, 경고 다음
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate, times(2)).convertAndSend(eq("/topic/chats/1"), payloadCaptor.capture());

        List<Object> payloads = payloadCaptor.getAllValues();
        assertThat(payloads.get(0)).isInstanceOf(ChatMessageResponseDTO.class);
        assertThat(payloads.get(1)).isInstanceOf(WarningIssuedMessage.class);

        WarningIssuedMessage warning = (WarningIssuedMessage) payloads.get(1);
        assertThat(warning.getType()).isEqualTo("WARNING_ISSUED");
        assertThat(warning.getWarningId()).isEqualTo(501L);
        assertThat(warning.getWarningCount()).isEqualTo(1);
        // 기본 정책(2회)의 첫 경고 문구 (REQ-016, 정책 4장)
        assertThat(warning.getMessage()).contains("한 번 더 폭언 시 상담이 종료되고 이용이 제한될 수 있습니다");
        // 경고에 원문이 섞이지 않음
        assertThat(warning.getMessage()).doesNotContain("병신");
    }

    @Test
    @DisplayName("종료 기준에 도달한 폭언에는 경고 알림을 보내지 않는다")
    void noWarningWhenShouldTerminate() {

        // 1. 준비
        ConsultationRepository consultationRepository = mock(ConsultationRepository.class);
        MessageRepository messageRepository = mock(MessageRepository.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        ModerationService moderationService = mock(ModerationService.class);
        ChatService chatService = abusiveMessageSetUp(
            consultationRepository, messageRepository, messagingTemplate, moderationService);

        // 경고 2회, 종료 기준 2회 → 종료 대상 (종료 알림은 MOD-03 이 보낸다)
        when(moderationService.applyAutoWarning(1L, 10L))
            .thenReturn(Optional.of(new WarningResult(1L, 502L, 2, 2, true)));

        // 2. 실행
        chatService.sendMessage(1L, abusiveRequest());

        // 3. 확인 - 마스킹된 메시지 1번만 발행, 경고 알림 없음
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/chats/1"), any(ChatMessageResponseDTO.class));
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(WarningIssuedMessage.class));
    }

    @Test
    @DisplayName("정상 메시지에는 경고를 만들지 않는다")
    void noWarningForNormalMessage() {

        // 1. 준비
        ConsultationRepository consultationRepository = mock(ConsultationRepository.class);
        MessageRepository messageRepository = mock(MessageRepository.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        ModerationService moderationService = mock(ModerationService.class);
        Consultation consultation = mock(Consultation.class);

        when(consultationRepository.findWithLockByConsultationId(1L)).thenReturn(Optional.of(consultation));
        when(consultation.getStatus()).thenReturn(ConsultationStatus.IN_PROGRESS);
        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(moderationService.processMessage("요금 문의드립니다"))
            .thenReturn(AbuseFilterResult.normal("요금 문의드립니다"));

        ChatMessageRequestDTO request = new ChatMessageRequestDTO();
        request.setSenderType("CUSTOMER");
        request.setSenderId(100L);
        request.setMessageContent("요금 문의드립니다");

        ChatService chatService = new ChatService(
            messageRepository,
            messagingTemplate,
            consultationRepository,
            eventPublisher,
            moderationService,
            mock(MemberRepository.class)
        );

        // 2. 실행
        chatService.sendMessage(1L, request);

        // 3. 확인 - 원문 그대로, NORMAL 저장. 원문 저장·경고 없음
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getContent()).isEqualTo("요금 문의드립니다");
        assertThat(messageCaptor.getValue().getAbuseStatus()).isEqualTo("NORMAL");

        verify(moderationService, never()).saveOriginal(anyLong(), anyLong(), any());
        verify(moderationService, never()).applyAutoWarning(anyLong(), anyLong());
    }

    // 경고 테스트 공통 준비: 상담 중 고객이 "병신같네" 를 보내고, 원문은 abuse_log_id 10 으로 저장된다
    private ChatService abusiveMessageSetUp(
        ConsultationRepository consultationRepository,
        MessageRepository messageRepository,
        SimpMessagingTemplate messagingTemplate,
        ModerationService moderationService) {

        Consultation consultation = mock(Consultation.class);
        AbuseLog abuseLog = mock(AbuseLog.class);

        when(consultationRepository.findWithLockByConsultationId(1L)).thenReturn(Optional.of(consultation));
        when(consultation.getStatus()).thenReturn(ConsultationStatus.IN_PROGRESS);
        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(moderationService.processMessage("병신같네"))
            .thenReturn(AbuseFilterResult.abusive("***같네"));
        when(abuseLog.getAbuseLogId()).thenReturn(10L);
        when(moderationService.saveOriginal(anyLong(), anyLong(), any())).thenReturn(abuseLog);

        return new ChatService(
            messageRepository,
            messagingTemplate,
            consultationRepository,
            mock(ApplicationEventPublisher.class),
            moderationService,
            mock(MemberRepository.class)
        );
    }

    private ChatMessageRequestDTO abusiveRequest() {
        ChatMessageRequestDTO request = new ChatMessageRequestDTO();
        request.setSenderType("CUSTOMER");
        request.setSenderId(100L);
        request.setMessageContent("병신같네");
        return request;
    }

    // ===== MOD-03 폭언 누적 자동 종료 =====

    @Test
    @DisplayName("종료 기준에 도달하면 경고 알림 없이 종료 알림만 보낸다")
    void abuseReachesThreshold_sendsTerminatedOnly() {

        // 1. 준비
        ConsultationRepository consultationRepository = mock(ConsultationRepository.class);
        MessageRepository messageRepository = mock(MessageRepository.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        ModerationService moderationService = mock(ModerationService.class);
        ChatService chatService = abusiveMessageSetUp(
            consultationRepository, messageRepository, messagingTemplate, moderationService);

        // 경고 2회, 종료 기준 2회 → 종료 대상
        when(moderationService.applyAutoWarning(1L, 10L))
            .thenReturn(Optional.of(new WarningResult(1L, 502L, 2, 2, true)));
        LocalDateTime closedAt = LocalDateTime.of(2026, 11, 17, 14, 30);
        when(moderationService.terminateForAbuse(1L)).thenReturn(Optional.of(new TerminationResult(
            1L, ConsultationStatus.TERMINATED, CloseReason.ABUSE_AUTO, ClosedBy.SYSTEM,
            closedAt, 7, closedAt.plusDays(7))));

        // 2. 실행
        chatService.sendMessage(1L, abusiveRequest());

        // 3. 확인 - 발행 2번: 마스킹된 메시지 먼저, 종료 알림 다음. 경고 알림 없음
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate, times(2)).convertAndSend(eq("/topic/chats/1"), payloadCaptor.capture());

        List<Object> payloads = payloadCaptor.getAllValues();
        assertThat(payloads.get(0)).isInstanceOf(ChatMessageResponseDTO.class);
        assertThat(payloads.get(1)).isInstanceOf(ConsultationTerminatedMessage.class);
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(WarningIssuedMessage.class));

        ConsultationTerminatedMessage terminated = (ConsultationTerminatedMessage) payloads.get(1);
        assertThat(terminated.getType()).isEqualTo("CONSULTATION_TERMINATED");
        assertThat(terminated.getStatus()).isEqualTo("TERMINATED");
        assertThat(terminated.getSuspendDays()).isEqualTo(7);
        assertThat(terminated.getSuspendEndAt()).isEqualTo(closedAt.plusDays(7));
        // 종료 안내 문구 (policy.md 6장), 원문이 섞이지 않음
        assertThat(terminated.getReason())
            .isEqualTo("반복된 폭언으로 상담이 종료되었습니다. 7일간 상담을 이용할 수 없습니다.");
        assertThat(terminated.getReason()).doesNotContain("병신");
    }

    @Test
    @DisplayName("이미 종료된 상담이면 종료 알림을 보내지 않는다")
    void abuseReachesThreshold_alreadyClosed_sendsNothing() {

        // 1. 준비
        ConsultationRepository consultationRepository = mock(ConsultationRepository.class);
        MessageRepository messageRepository = mock(MessageRepository.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        ModerationService moderationService = mock(ModerationService.class);
        ChatService chatService = abusiveMessageSetUp(
            consultationRepository, messageRepository, messagingTemplate, moderationService);

        when(moderationService.applyAutoWarning(1L, 10L))
            .thenReturn(Optional.of(new WarningResult(1L, 502L, 2, 2, true)));
        // 상담원이 먼저 종료하는 등 이미 종료된 상담
        when(moderationService.terminateForAbuse(1L)).thenReturn(Optional.empty());

        // 2. 실행
        chatService.sendMessage(1L, abusiveRequest());

        // 3. 확인 - 마스킹된 메시지만 발행
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/chats/1"), any(ChatMessageResponseDTO.class));
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(ConsultationTerminatedMessage.class));
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(WarningIssuedMessage.class));
    }

    @Test
    @DisplayName("종료 기준 전이면 자동 종료하지 않는다")
    void abuseBelowThreshold_doesNotTerminate() {

        // 1. 준비
        ConsultationRepository consultationRepository = mock(ConsultationRepository.class);
        MessageRepository messageRepository = mock(MessageRepository.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        ModerationService moderationService = mock(ModerationService.class);
        ChatService chatService = abusiveMessageSetUp(
            consultationRepository, messageRepository, messagingTemplate, moderationService);

        // 경고 1회, 종료 기준 2회 → 아직 종료 아님
        when(moderationService.applyAutoWarning(1L, 10L))
            .thenReturn(Optional.of(new WarningResult(1L, 501L, 1, 2, false)));

        // 2. 실행
        chatService.sendMessage(1L, abusiveRequest());

        // 3. 확인
        verify(moderationService, never()).terminateForAbuse(anyLong());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(ConsultationTerminatedMessage.class));
    }
}
