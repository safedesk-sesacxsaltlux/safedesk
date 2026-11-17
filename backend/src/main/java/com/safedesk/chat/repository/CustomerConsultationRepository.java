package com.safedesk.chat.repository;

import com.safedesk.chat.entity.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

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

    // CUS-01: 같은 고객의 상담 시작 요청을 한 줄로 세운다. 버튼을 두 번 눌러도 상담이 두 개 생기지 않게.
    // consultation 에는 잠글 행이 아직 없어서 member 행을 잠근다. 트랜잭션이 끝나면 풀린다.
    @Query(value = "SELECT member_id FROM member WHERE member_id = :customerId FOR UPDATE", nativeQuery = true)
    Optional<Long> lockCustomer(@Param("customerId") long customerId);

    // CUS-01: 지금 정지 중인 제재 (수동 해제되지 않았고 해제 일시가 아직 안 지남)
    // Sanction 엔티티가 MOD-04(#57) 담당이라 엔티티 대신 테이블을 직접 읽는다.
    // 안내 문구에 쓸 위반 상담의 종료 사유도 같이 가져온다 (HIS-02 reason 과 같은 기준).
    @Query(value = """
        SELECT s.end_at AS releaseAt, c.close_reason AS closeReason
        FROM sanction s
        JOIN violation v ON v.violation_id = s.violation_id
        JOIN consultation c ON c.consultation_id = v.consultation_id
        WHERE s.customer_id = :customerId
          AND s.released_at IS NULL
          AND s.end_at > :now
        ORDER BY s.end_at DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<ActiveSanctionRow> findActiveSanction(@Param("customerId") long customerId,
                                                   @Param("now") LocalDateTime now);

    // CUS-01: 아직 끝나지 않은 상담 (AI_CHAT, QUEUED, IN_PROGRESS)
    Optional<Consultation> findFirstByCustomerIdAndStatusIn(long customerId, Collection<String> statuses);

}
