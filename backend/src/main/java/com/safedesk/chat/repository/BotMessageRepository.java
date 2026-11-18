package com.safedesk.chat.repository;

import com.safedesk.chat.entity.Message;
import org.springframework.data.repository.Repository;

import java.util.Optional;

/**
 * AI 상담(CUS-02) 전용 메시지 조회.
 * MessageRepository 는 상담원 채팅 담당 파일이라 같이 고치면 충돌이 나서 따로 둔다. 같은 message 테이블을 쓴다.
 */
public interface BotMessageRepository extends Repository<Message, Long> {

    // 반복 질문 판정용: 이번 메시지 바로 앞의 고객 메시지
    Optional<Message> findFirstByConsultationIdAndSenderTypeAndMessageIdLessThanOrderByMessageIdDesc(
        long consultationId, String senderType, long messageId);
}
