package com.safedesk.chat.service;

import com.safedesk.ai.AiFailureType;
import com.safedesk.ai.AiGateway;
import com.safedesk.ai.dto.AiReplyRequest;
import com.safedesk.ai.dto.AiResult;
import com.safedesk.chat.dto.BotMessageResponseDTO;
import com.safedesk.chat.entity.AnswerSource;
import com.safedesk.chat.entity.Consultation;
import com.safedesk.chat.entity.HandoffReason;
import com.safedesk.chat.entity.Message;
import com.safedesk.chat.repository.BotMessageRepository;
import com.safedesk.chat.repository.ConsultationRepository;
import com.safedesk.chat.repository.CustomerConsultationRepository;
import com.safedesk.chat.repository.MessageRepository;
import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.faq.FaqMatcher;
import com.safedesk.faq.dto.FaqMatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BotMessageServiceTest {

    private static final long CHAT_ID = 1L;
    private static final long CUSTOMER_ID = 100L;
    private static final FaqMatch WATER_FAQ = new FaqMatch(3L, "상하수도", "수도요금 납부 방법", "청구서, 자동납부로 낼 수 있습니다.", 4);

    private ConsultationRepository consultationRepository;
    private CustomerConsultationRepository customerConsultationRepository;
    private MessageRepository messageRepository;
    private BotMessageRepository botMessageRepository;
    private FaqMatcher faqMatcher;
    private AiGateway aiGateway;
    private BotMessageService service;
    private Consultation consultation;

    @BeforeEach
    void setUp() {
        consultationRepository = mock(ConsultationRepository.class);
        customerConsultationRepository = mock(CustomerConsultationRepository.class);
        messageRepository = mock(MessageRepository.class);
        botMessageRepository = mock(BotMessageRepository.class);
        faqMatcher = mock(FaqMatcher.class);
        aiGateway = mock(AiGateway.class);
        // 트랜잭션 매니저는 가짜라 실제 트랜잭션 없이 콜백만 실행된다
        TransactionTemplate transactionTemplate = new TransactionTemplate(mock(PlatformTransactionManager.class));
        service = new BotMessageService(consultationRepository, customerConsultationRepository, messageRepository, botMessageRepository,
            faqMatcher, aiGateway, transactionTemplate);

        consultation = Consultation.start(CUSTOMER_ID, 1L);   // AI_CHAT
        when(consultationRepository.findById(CHAT_ID)).thenReturn(Optional.of(consultation));
        // ③의 잠금 조회는 그 시점의 상태를 돌려준다 (AI 호출 중에 바뀐 상태도 반영)
        when(customerConsultationRepository.lockStatus(CHAT_ID)).thenAnswer(invocation -> Optional.of(consultation.getStatus()));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(faqMatcher.match(anyString())).thenReturn(Optional.empty());
        when(botMessageRepository.findFirstByConsultationIdAndSenderTypeAndMessageIdLessThanOrderByMessageIdDesc(
            anyLong(), anyString(), anyLong())).thenReturn(Optional.empty());
    }

    private void givenAiSuccess() {
        when(aiGateway.reply(any())).thenReturn(AiResult.success("[목업] 답변", 700));
    }

    private void givenAiFailure() {
        when(aiGateway.reply(any())).thenReturn(AiResult.failure(AiFailureType.TIMEOUT, 10_000));
    }

    @Test
    @DisplayName("AI 가 답하면 AI 답변을 저장하고, 매칭된 FAQ 를 상담에 기록한다")
    void sendWhenAiSucceeds() {
        givenAiSuccess();
        when(faqMatcher.match("수도요금 어떻게 내요?")).thenReturn(Optional.of(WATER_FAQ));

        BotMessageResponseDTO response = service.send(CHAT_ID, CUSTOMER_ID, "수도요금 어떻게 내요?");

        assertThat(response.answerSource()).isEqualTo(AnswerSource.AI);
        assertThat(response.botMessage().content()).isEqualTo("[목업] 답변");
        assertThat(response.botMessage().senderType()).isEqualTo("AI");
        assertThat(response.customerMessage().content()).isEqualTo("수도요금 어떻게 내요?");
        assertThat(response.faqId()).isEqualTo(3L);
        assertThat(response.handoffSuggested()).isFalse();
        verify(customerConsultationRepository).recordFirstFaqMatch(CHAT_ID, 3L, "상하수도");
    }

    @Test
    @DisplayName("AI 에 매칭된 FAQ 답변을 함께 넘긴다 (목업이 NORMAL 모드에서 이 답변을 쓴다)")
    void sendPassesFaqAnswerToAi() {
        givenAiSuccess();
        when(faqMatcher.match("수도요금 어떻게 내요?")).thenReturn(Optional.of(WATER_FAQ));

        service.send(CHAT_ID, CUSTOMER_ID, "수도요금 어떻게 내요?");

        verify(aiGateway).reply(new AiReplyRequest(CHAT_ID, "수도요금 어떻게 내요?", WATER_FAQ.answer()));
    }

    @Test
    @DisplayName("AI 가 실패해도 FAQ 가 맞으면 FAQ 답변으로 대신한다 (오류 아님)")
    void sendWhenAiFailsWithFaq() {
        givenAiFailure();
        when(faqMatcher.match("수도요금 어떻게 내요?")).thenReturn(Optional.of(WATER_FAQ));

        BotMessageResponseDTO response = service.send(CHAT_ID, CUSTOMER_ID, "수도요금 어떻게 내요?");

        assertThat(response.answerSource()).isEqualTo(AnswerSource.FAQ_FALLBACK);
        assertThat(response.botMessage().content()).isEqualTo(WATER_FAQ.answer());
        assertThat(response.handoffSuggested()).isFalse();
    }

    @Test
    @DisplayName("AI 가 실패하고 맞는 FAQ 도 없으면 NO_ANSWER + 상담원 연결 제안 (체크리스트 E2)")
    void sendWhenAiFailsWithoutFaq() {
        givenAiFailure();

        BotMessageResponseDTO response = service.send(CHAT_ID, CUSTOMER_ID, "오늘 날씨 어때요?");

        assertThat(response.answerSource()).isEqualTo(AnswerSource.NO_ANSWER);
        assertThat(response.botMessage().content()).isEqualTo(BotMessageService.MSG_NO_ANSWER);
        assertThat(response.handoffSuggested()).isTrue();
        assertThat(response.handoffReason()).isEqualTo(HandoffReason.NO_FAQ_MATCH);
        verify(messageRepository, times(2)).save(any());   // 고객 메시지 + 봇 메시지 모두 남는다
    }

    @Test
    @DisplayName("고객이 상담원을 찾으면 AI 를 부르지 않고 연결을 제안한다")
    void sendWhenCustomerAsksForAgent() {
        BotMessageResponseDTO response = service.send(CHAT_ID, CUSTOMER_ID, "상담원 연결해 주세요");

        assertThat(response.handoffSuggested()).isTrue();
        assertThat(response.handoffReason()).isEqualTo(HandoffReason.CUSTOMER_REQUEST);
        verify(aiGateway, never()).reply(any());
    }

    @Test
    @DisplayName("직전 질문과 같은 FAQ 가 다시 매칭되면 AI 를 부르지 않고 연결을 제안한다")
    void sendWhenSameFaqRepeated() {
        Message previous = new Message();
        previous.setContent("수도요금 납부 방법 알려줘");
        when(botMessageRepository.findFirstByConsultationIdAndSenderTypeAndMessageIdLessThanOrderByMessageIdDesc(
            eq(CHAT_ID), eq("CUSTOMER"), anyLong())).thenReturn(Optional.of(previous));
        when(faqMatcher.match(anyString())).thenReturn(Optional.of(WATER_FAQ));

        BotMessageResponseDTO response = service.send(CHAT_ID, CUSTOMER_ID, "수도요금 어떻게 내요?");

        assertThat(response.handoffReason()).isEqualTo(HandoffReason.REPEATED_QUESTION);
        verify(aiGateway, never()).reply(any());
    }

    @Test
    @DisplayName("「직원」이 들어간 일반 문의는 상담원 요청으로 보지 않는다")
    void sendWhenStaffMentionedInInquiry() {
        givenAiSuccess();

        BotMessageResponseDTO response = service.send(CHAT_ID, CUSTOMER_ID, "역 직원이 불친절했어요");

        assertThat(response.handoffSuggested()).isFalse();
        verify(aiGateway).reply(any());
    }

    @Test
    @DisplayName("AI 응답을 기다리는 동안 상담원 연결을 누르면 봇 메시지를 저장하지 않고 CHAT_NOT_IN_BOT")
    void sendWhenHandedOffDuringAiCall() {
        when(faqMatcher.match(anyString())).thenReturn(Optional.of(WATER_FAQ));
        // AI 호출 중에 고객이 상담원 연결(CUS-03)을 누른 상황
        when(aiGateway.reply(any())).thenAnswer(invocation -> {
            consultation.handoff();
            return AiResult.success("[목업] 답변", 700);
        });

        assertErrorCode(ErrorCode.CHAT_NOT_IN_BOT);
        verify(messageRepository, times(1)).save(any());   // 고객 메시지만 남는다
        assertThat(consultation.getStatus()).isEqualTo("QUEUED");
        verify(customerConsultationRepository, never()).recordFirstFaqMatch(anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("AI 응답을 기다리는 동안 상담을 종료하면 봇 메시지를 저장하지 않고 CHAT_CLOSED")
    void sendWhenClosedDuringAiCall() {
        when(aiGateway.reply(any())).thenAnswer(invocation -> {
            consultation.close("CUSTOMER", "NORMAL");
            return AiResult.success("[목업] 답변", 700);
        });

        assertErrorCode(ErrorCode.CHAT_CLOSED);
        verify(messageRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("상담원 대기 중이면 CHAT_NOT_IN_BOT, 메시지를 저장하지 않는다")
    void sendWhenQueued() {
        consultation.handoff();   // QUEUED

        assertErrorCode(ErrorCode.CHAT_NOT_IN_BOT);
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("종료된 상담이면 CHAT_CLOSED")
    void sendWhenClosed() {
        consultation.close("CUSTOMER", "NORMAL");

        assertErrorCode(ErrorCode.CHAT_CLOSED);
    }

    @Test
    @DisplayName("다른 고객의 상담이면 CHAT_ACCESS_DENIED")
    void sendWhenNotOwner() {
        when(consultationRepository.findById(CHAT_ID)).thenReturn(Optional.of(Consultation.start(999L, 1L)));

        assertErrorCode(ErrorCode.CHAT_ACCESS_DENIED);
    }

    @Test
    @DisplayName("없는 상담이면 CHAT_NOT_FOUND")
    void sendWhenNotFound() {
        when(consultationRepository.findById(CHAT_ID)).thenReturn(Optional.empty());

        assertErrorCode(ErrorCode.CHAT_NOT_FOUND);
    }

    private void assertErrorCode(ErrorCode expected) {
        assertThatThrownBy(() -> service.send(CHAT_ID, CUSTOMER_ID, "질문"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(expected);
    }
}
