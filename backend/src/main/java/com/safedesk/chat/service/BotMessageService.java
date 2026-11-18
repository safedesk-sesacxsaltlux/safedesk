package com.safedesk.chat.service;

import com.safedesk.ai.AiGateway;
import com.safedesk.ai.dto.AiReplyRequest;
import com.safedesk.ai.dto.AiResult;
import com.safedesk.chat.dto.BotMessageItemDTO;
import com.safedesk.chat.dto.BotMessageResponseDTO;
import com.safedesk.chat.entity.AnswerSource;
import com.safedesk.chat.entity.Consultation;
import com.safedesk.chat.entity.ConsultationStatus;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * CUS-02 AI 상담 메시지 전송·응답 (docs/api/customer-chat.md).
 *
 * 메서드 전체에 @Transactional 을 붙이지 않는다.
 * AI 호출은 최대 10초가 걸리는데, 그동안 트랜잭션을 열어 두면 DB 커넥션을 붙잡고 있게 된다.
 * 그래서 「① 고객 메시지 저장」과 「③ 봇 메시지 저장」만 각각 짧은 트랜잭션으로 묶고, ② AI 호출은 트랜잭션 밖에서 한다.
 * ①을 먼저 커밋하므로 AI 가 실패해도 고객이 보낸 문장은 남는다 (명세 설계 이유).
 */
@Slf4j
@Service
public class BotMessageService {

    static final String SENDER_CUSTOMER = "CUSTOMER";
    // 명세 예시와 contract.md D-05 제안은 BOT 이지만, DB CHECK 제약(ck_message_sender_type)과 HIS-06 이 AI 를 쓴다
    static final String SENDER_BOT = "AI";

    // 공백을 지운 문장에 이 말이 있으면 상담원을 원하는 것으로 본다.
    // 「직원」「사람과」만으로는 잡지 않는다. 「역 직원이 불친절했어요」 같은 일반 문의(FAQ)까지 상담원 요청으로 빠지기 때문
    private static final List<String> AGENT_REQUEST_WORDS =
        List.of("상담원", "상담사", "사람연결", "사람이랑", "직원연결");

    static final String MSG_CUSTOMER_REQUEST = "상담원을 연결해 드릴까요? 아래 ‘상담원 연결’ 버튼을 눌러 주세요.";
    static final String MSG_NO_ANSWER = "정확한 답변을 위해 상담원을 연결해 드릴까요?";
    static final String MSG_REPEATED = "같은 문의가 반복되고 있어요. 상담원을 연결해 드릴까요?";

    private final ConsultationRepository consultationRepository;
    private final CustomerConsultationRepository customerConsultationRepository;
    private final MessageRepository messageRepository;
    private final BotMessageRepository botMessageRepository;
    private final FaqMatcher faqMatcher;
    private final AiGateway aiGateway;
    private final TransactionTemplate transactionTemplate;

    public BotMessageService(ConsultationRepository consultationRepository,
                             CustomerConsultationRepository customerConsultationRepository,
                             MessageRepository messageRepository,
                             BotMessageRepository botMessageRepository,
                             FaqMatcher faqMatcher,
                             AiGateway aiGateway,
                             TransactionTemplate transactionTemplate) {
        this.consultationRepository = consultationRepository;
        this.customerConsultationRepository = customerConsultationRepository;
        this.messageRepository = messageRepository;
        this.botMessageRepository = botMessageRepository;
        this.faqMatcher = faqMatcher;
        this.aiGateway = aiGateway;
        this.transactionTemplate = transactionTemplate;
    }

    public BotMessageResponseDTO send(long chatId, long customerId, String content) {
        // ① 확인 + 고객 메시지 저장 (짧은 트랜잭션)
        Message customerMessage = transactionTemplate.execute(status -> {
            Consultation consultation = findMyBotChat(chatId, customerId);
            return messageRepository.save(newMessage(consultation.getConsultationId(), SENDER_CUSTOMER, customerId, content));
        });

        // 반복 판정·대체 답변·문의 유형 저장이 모두 같은 매칭 결과를 쓴다
        Optional<FaqMatch> match = faqMatcher.match(content);

        // ② 답변 만들기. 이관 조건을 AI 호출보다 먼저 본다 (상담원을 원하는 고객에게 AI 답변을 또 보이지 않게)
        Reply reply = decideHandoff(chatId, customerMessage.getMessageId(), content, match)
            .orElseGet(() -> askAi(chatId, content, match));

        // ③ 봇 메시지 저장 + 처음 매칭된 FAQ 기록 (짧은 트랜잭션)
        // AI 를 기다리는 동안 고객이 상담원 연결(CUS-03)·종료(CUS-04)를 눌렀을 수 있어서 상태를 다시 본다.
        // 상태는 행을 잠그고 값으로 읽고, FAQ 는 두 컬럼만 UPDATE 한다. ①에서 읽은 Consultation 엔티티는 쓰지 않는다
        // (open-in-view 로 예전 상태가 남아 있어, 엔티티를 고치면 그사이 바뀐 QUEUED·CLOSED 를 AI_CHAT 으로 되돌린다).
        // 상태가 바뀌었으면 ①과 같은 에러를 준다 (고객 메시지는 ①에서 이미 저장됨).
        Message botMessage = transactionTemplate.execute(status -> {
            String current = customerConsultationRepository.lockStatus(chatId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_NOT_FOUND));
            requireBotChat(current);
            match.ifPresent(faq -> customerConsultationRepository.recordFirstFaqMatch(chatId, faq.faqId(), faq.category()));
            return messageRepository.save(newMessage(chatId, SENDER_BOT, null, reply.content()));
        });

        return new BotMessageResponseDTO(
            BotMessageItemDTO.from(customerMessage),
            BotMessageItemDTO.from(botMessage),
            reply.answerSource(),
            match.map(FaqMatch::faqId).orElse(null),
            reply.handoffReason() != null,
            reply.handoffReason());
    }

    private Consultation findMyBotChat(long chatId, long customerId) {
        Consultation consultation = consultationRepository.findById(chatId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_NOT_FOUND));
        if (consultation.getCustomerId() != customerId) {
            throw new BusinessException(ErrorCode.CHAT_ACCESS_DENIED);
        }
        requireBotChat(consultation.getStatus());
        return consultation;
    }

    // AI 응대 중(AI_CHAT)이 아니면 상태에 맞는 에러
    private static void requireBotChat(String status) {
        switch (status) {
            case ConsultationStatus.AI_CHAT -> { }
            case ConsultationStatus.QUEUED, ConsultationStatus.IN_PROGRESS ->
                throw new BusinessException(ErrorCode.CHAT_NOT_IN_BOT);
            default -> throw new BusinessException(ErrorCode.CHAT_CLOSED);
        }
    }

    // 상담원 연결을 제안해야 하면 그 답변, 아니면 empty
    private Optional<Reply> decideHandoff(long chatId, long messageId, String content, Optional<FaqMatch> match) {
        if (asksForAgent(content)) {
            return Optional.of(Reply.handoff(MSG_CUSTOMER_REQUEST, HandoffReason.CUSTOMER_REQUEST));
        }
        // 문장 대신 FAQ 로 비교한다. 표현만 바꾼 같은 질문도 잡으려고 (명세 설계 이유)
        if (match.isPresent() && match.get().faqId().equals(previousFaqId(chatId, messageId))) {
            return Optional.of(Reply.handoff(MSG_REPEATED, HandoffReason.REPEATED_QUESTION));
        }
        return Optional.empty();
    }

    private Long previousFaqId(long chatId, long messageId) {
        return botMessageRepository
            .findFirstByConsultationIdAndSenderTypeAndMessageIdLessThanOrderByMessageIdDesc(chatId, SENDER_CUSTOMER, messageId)
            .flatMap(previous -> faqMatcher.match(previous.getContent()))
            .map(FaqMatch::faqId)
            .orElse(null);
    }

    private boolean asksForAgent(String content) {
        String text = content.replaceAll("\\s+", "");
        return AGENT_REQUEST_WORDS.stream().anyMatch(text::contains);
    }

    // AI 실패는 오류로 보내지 않고 FAQ 답변으로 대신한다 (NFR-006, 정책 8장)
    private Reply askAi(long chatId, String content, Optional<FaqMatch> match) {
        AiResult result = aiGateway.reply(
            new AiReplyRequest(chatId, content, match.map(FaqMatch::answer).orElse(null)));

        Reply reply;
        if (result.success()) {
            reply = Reply.answer(result.content(), AnswerSource.AI);
        } else if (match.isPresent()) {
            reply = Reply.answer(match.get().answer(), AnswerSource.FAQ_FALLBACK);
        } else {
            reply = new Reply(MSG_NO_ANSWER, AnswerSource.NO_ANSWER, HandoffReason.NO_FAQ_MATCH);
        }

        // AI 호출 기록은 DB 가 아니라 서버 로그로 남긴다 (contract.md D-12). 고객 질문·답변 원문은 남기지 않는다
        log.info("AI 호출: chatId={}, success={}, failureType={}, latencyMs={}, answerSource={}",
            chatId, result.success(), result.failureType(), result.latencyMs(), reply.answerSource());
        return reply;
    }

    private static Message newMessage(long chatId, String senderType, Long senderId, String content) {
        Message message = new Message();
        message.setConsultationId(chatId);
        message.setSenderType(senderType);
        message.setSenderId(senderId);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        return message;
    }

    // 봇 답변 내용과 출처. handoffReason 이 있으면 상담원 연결을 제안한다
    private record Reply(String content, String answerSource, String handoffReason) {

        static Reply answer(String content, String answerSource) {
            return new Reply(content, answerSource, null);
        }

        // 상담원 연결 제안은 답을 주지 않은 것이므로 NO_ANSWER 로 둔다
        static Reply handoff(String content, String handoffReason) {
            return new Reply(content, AnswerSource.NO_ANSWER, Objects.requireNonNull(handoffReason));
        }
    }
}
