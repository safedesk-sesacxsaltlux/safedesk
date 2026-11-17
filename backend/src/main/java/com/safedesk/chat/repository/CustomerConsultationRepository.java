package com.safedesk.chat.repository;

import com.safedesk.chat.entity.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

/**
 * 고객 상담(CUS-01~04) 전용 쿼리.
 * ConsultationRepository(정혜영 담당)를 같이 고치면 충돌이 나서 따로 둔다. 같은 consultation 테이블을 쓴다.
 */
public interface CustomerConsultationRepository extends JpaRepository<Consultation, Long> {

    // 상담원 수락(IN_PROGRESS)과 동시에 들어와도 한쪽만 성공하도록 상태 조건을 WHERE 에 둔다.
    // 반환값: 바뀐 행 수 (0 이면 이미 수락됐거나 종료된 상담)
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Consultation c SET c.status = 'CLOSED', c.closeReason = :closeReason, " +
        "c.closedBy = 'CUSTOMER', c.closedAt = :now " +
        "WHERE c.consultationId = :id AND c.status IN ('AI_CHAT', 'QUEUED')")
    int closeByCustomer(@Param("id") long id,
                        @Param("closeReason") String closeReason,
                        @Param("now") LocalDateTime now);

}
