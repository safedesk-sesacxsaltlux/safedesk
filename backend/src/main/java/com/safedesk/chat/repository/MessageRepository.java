package com.safedesk.chat.repository;

import com.safedesk.chat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConsultationIdOrderByCreatedAtAsc(long consultationId);
}
