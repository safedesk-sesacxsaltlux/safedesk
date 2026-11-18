package com.safedesk.chat.repository;

import com.safedesk.chat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    // 같은 밀리초에 저장된 메시지도 순서가 항상 같도록 messageId 를 보조 정렬로 둔다 (PR #66 리뷰)
    List<Message> findByConsultationIdOrderByCreatedAtAscMessageIdAsc(long consultationId);
}
