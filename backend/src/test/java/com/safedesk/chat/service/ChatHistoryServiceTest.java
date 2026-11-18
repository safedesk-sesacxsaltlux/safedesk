package com.safedesk.chat.service;

import com.safedesk.chat.dto.ChatDetailResponseDTO;
import com.safedesk.chat.dto.ChatHistoryResponseDTO;
import com.safedesk.chat.entity.Consultation;
import com.safedesk.chat.entity.ConsultationStatus;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatHistoryServiceTest {

    private static final LocalDateTime QUEUED_AT = LocalDateTime.of(2026, 11, 18, 10, 0);

    private final MessageRepository messageRepository = mock(MessageRepository.class);
    private final ConsultationRepository consultationRepository = mock(ConsultationRepository.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final ViolationRepository violationRepository = mock(ViolationRepository.class);
    private final WarningRepository warningRepository = mock(WarningRepository.class);
    private final SanctionPolicyRepository policyRepository = mock(SanctionPolicyRepository.class);
    private final ChatHistoryService service = new ChatHistoryService(
        messageRepository, consultationRepository, memberRepository,
        violationRepository, warningRepository, policyRepository);

    private final Consultation consultation = mock(Consultation.class);

    @BeforeEach
    void setUp() {
        // 고객 100, 수락한 상담원 10
        when(consultationRepository.findById(1L)).thenReturn(Optional.of(consultation));
        when(consultation.getConsultationId()).thenReturn(1L);
        when(consultation.getCustomerId()).thenReturn(100L);
        when(consultation.getAgentId()).thenReturn(10L);
        when(consultation.getStatus()).thenReturn(ConsultationStatus.IN_PROGRESS);
        when(consultation.getQueuedAt()).thenReturn(QUEUED_AT);
        when(consultation.getAcceptedAt()).thenReturn(QUEUED_AT.plusSeconds(30));
        when(consultation.getPolicyId()).thenReturn(3L);
    }

    // ===== API-004 메시지 내역 =====

    @Test
    @DisplayName("상담원과 고객 모두 메시지 내역을 볼 수 있다 (재연결 NFR-008)")
    void historyForParticipants() {
        List<Message> messages = List.of(message(1, "CUSTOMER", 0), message(2, "AGENT", 5));
        when(messageRepository.findByConsultationIdOrderByCreatedAtAscMessageIdAsc(1L)).thenReturn(messages);

        assertThat(service.getChatHistory(1L, 10L)).hasSize(2);
        assertThat(service.getChatHistory(1L, 100L)).extracting(ChatHistoryResponseDTO::messageId)
            .containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("다른 사람은 메시지 내역을 볼 수 없다 (NFR-004)")
    void historyForOutsider() {
        assertThatThrownBy(() -> service.getChatHistory(1L, 999L))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.CHAT_ACCESS_DENIED);
    }

    @Test
    @DisplayName("없는 상담이면 CHAT_NOT_FOUND")
    void historyNotFound() {
        when(consultationRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getChatHistory(2L, 10L))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.CHAT_NOT_FOUND);
    }

    // ===== API-007 AI 응대 내역 =====

    @Test
    @DisplayName("AI 응대 내역은 수락 전의 고객·AI 메시지만 준다")
    void aiMessagesBeforeHandoff() {
        List<Message> messages = List.of(
            message(1, "CUSTOMER", -120),
            message(2, "AI", 0),            // 연결 요청과 같은 초 (queued_at 은 초 단위로 잘림)
            message(3, "SYSTEM", -100),
            message(4, "CUSTOMER", 60),     // 상담원 연결 뒤
            message(5, "AGENT", 70));
        when(messageRepository.findByConsultationIdOrderByCreatedAtAscMessageIdAsc(1L)).thenReturn(messages);

        assertThat(service.getAiMessages(1L, 10L))
            .extracting(ChatHistoryResponseDTO::messageId)
            .containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("수락한 상담원이 아니면 AI 응대 내역을 볼 수 없다")
    void aiMessagesForOtherAgent() {
        assertThatThrownBy(() -> service.getAiMessages(1L, 20L))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.CHAT_ACCESS_DENIED);
        // 고객 본인도 이 API 는 쓰지 않는다
        assertThatThrownBy(() -> service.getAiMessages(1L, 100L))
            .isInstanceOf(BusinessException.class);
    }

    // ===== API-003 상세 =====

    @Test
    @DisplayName("상세에는 고객 누적 위반, 이번 상담 경고, 종료 기준이 담긴다")
    void detail() {
        Member customer = mock(Member.class);
        Member agent = mock(Member.class);
        SanctionPolicy policy = mock(SanctionPolicy.class);
        when(customer.getName()).thenReturn("홍길동");
        when(agent.getName()).thenReturn("김상담");
        when(memberRepository.findById(100L)).thenReturn(Optional.of(customer));
        when(memberRepository.findById(10L)).thenReturn(Optional.of(agent));
        when(violationRepository.countByCustomerId(100L)).thenReturn(1L);
        when(warningRepository.countByConsultationId(1L)).thenReturn(1L);
        when(policy.getCloseThreshold()).thenReturn(2);
        when(policyRepository.findByIdOrLatest(3L)).thenReturn(policy);

        ChatDetailResponseDTO detail = service.getChatDetail(1L, 10L);

        assertThat(detail.getCustomerName()).isEqualTo("홍길동");
        assertThat(detail.getAgentName()).isEqualTo("김상담");
        assertThat(detail.getConsultationStatus()).isEqualTo(ConsultationStatus.IN_PROGRESS);
        assertThat(detail.getCustomerViolationCount()).isEqualTo(1L);
        assertThat(detail.getWarningCount()).isEqualTo(1L);
        assertThat(detail.getCloseThreshold()).isEqualTo(2);
    }

    @Test
    @DisplayName("다른 사람은 상세를 볼 수 없다")
    void detailForOutsider() {
        assertThatThrownBy(() -> service.getChatDetail(1L, 999L))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.CHAT_ACCESS_DENIED);
    }

    private static Message message(long id, String senderType, int secondsFromQueued) {
        Message m = mock(Message.class);
        when(m.getMessageId()).thenReturn(id);
        when(m.getConsultationId()).thenReturn(1L);
        when(m.getSenderType()).thenReturn(senderType);
        when(m.getContent()).thenReturn("메시지 " + id);
        when(m.getCreatedAt()).thenReturn(QUEUED_AT.plusSeconds(secondsFromQueued));
        return m;
    }
}
