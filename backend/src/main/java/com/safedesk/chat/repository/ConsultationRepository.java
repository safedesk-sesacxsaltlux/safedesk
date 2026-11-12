package com.safedesk.chat.repository;

import com.safedesk.chat.entity.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    List<Consultation> findByStatusOrderByQueuedAtAsc(String status);
}
