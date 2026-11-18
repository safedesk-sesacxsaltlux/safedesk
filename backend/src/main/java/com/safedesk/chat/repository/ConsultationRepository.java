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

    // 상담원이 지금 진행 중인 상담 (API-008, 콘솔에서 진행 중 상담으로 돌아가기)
    Optional<Consultation> findFirstByAgentIdAndStatusOrderByAcceptedAtDesc(Long agentId, String status);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Consultation c SET c.status = 'IN_PROGRESS', c.agentId = :agentId, c.acceptedAt = :now " +
        "WHERE c.consultationId = :id AND c.status = 'QUEUED'")
    int accept(@Param("id") long id,
               @Param("agentId") Long agentId,
               @Param("now") LocalDateTime now);

    // 폭언 누적 자동 종료 (MOD-03). accept() 와 같은 조건부 UPDATE (contract.md 「상태 변경은 조건부 UPDATE」)
    //   - flushAutomatically: 같은 트랜잭션에서 저장한 메시지·원문·경고를 UPDATE 전에 DB 에 반영한다
    //   - clearAutomatically: UPDATE 뒤 영속성 컨텍스트를 비운다 → 그 뒤의 Consultation 엔티티는 옛날 상태라 다시 쓰지 않는다
    //   - 반환값(바뀐 행 수)이 1 일 때만 종료 성공. 상담원 종료(API-006)와 동시에 와도 한쪽만 성공한다
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Consultation c SET c.status = 'TERMINATED', c.closedAt = :now, " +
        "c.closedBy = 'SYSTEM', c.closeReason = 'ABUSE_AUTO' " +
        "WHERE c.consultationId = :id AND c.status = 'IN_PROGRESS'")
    int terminateForAbuse(@Param("id") long id, @Param("now") LocalDateTime now);

}
