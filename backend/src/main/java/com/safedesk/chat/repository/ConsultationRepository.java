package com.safedesk.chat.repository;

import com.safedesk.chat.entity.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

import java.util.List;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    // 상담 대기열을 조회하는 메서드
    List<Consultation> findByStatusOrderByQueuedAtAsc(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Consultation> findWithLockByConsultationId(long consultationId);

    boolean existsByAgentIdAndStatus(Long agentId, String status);

    Optional<Consultation> findFirstByAgentIdAndStatusOrderByClosedAtDesc(Long agentId, String status);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Consultation c SET c.status = 'IN_PROGRESS', c.agentId = :agentId, c.acceptedAt = :now " +
        "WHERE c.consultationId = :id AND c.status = 'QUEUED'")
    int accept(@Param("id") long id,
               @Param("agentId") Long agentId,
               @Param("now") LocalDateTime now);


}
