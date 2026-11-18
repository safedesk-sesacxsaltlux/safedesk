package com.safedesk.sanction.repository;

import com.safedesk.sanction.entity.Violation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * violation 저장소 (MOD-04).
 */
public interface ViolationRepository extends JpaRepository<Violation, Long> {

    /**
     * 같은 상담의 위반이 이미 있는지 (중복 방지, 처리 순서 1번).
     * exists 가 아니라 find 인 이유: 이미 있으면 기존 위반·정지로 결과를 만들어 돌려주기 때문.
     */
    Optional<Violation> findByConsultationId(Long consultationId);

    /**
     * 고객의 누적 위반 횟수 (처리 순서 3번).
     * 정지가 풀려도 초기화하지 않으므로 기간 조건 없이 전부 센다 (REQ-020).
     */
    long countByCustomerId(Long customerId);

    // API-ADM-06 이 제재가 몇 번째 위반인지
    long countByCustomerIdAndViolationIdLessThanEqual(Long customerId, Long violationId);

    // API-ADM-05 고객의 위반 전체
    @Query(value = """
            select v.violation_id    as violationId,
                   v.consultation_id as consultationId,
                   c.closed_by       as closedBy,
                   v.created_at      as violatedAt,
                   (select count(*) from abuse_log a
                     where a.consultation_id = v.consultation_id) as abusiveMessageCount,
                   s.suspend_days    as appliedDurationDays
            from violation v
            join consultation c on c.consultation_id = v.consultation_id
            left join sanction s on s.violation_id = v.violation_id
            where v.customer_id = :customerId
            order by v.violation_id
            """, nativeQuery = true)
    List<ViolationHistoryRow> findHistoryByCustomerId(@Param("customerId") Long customerId);

    // API-ADM-07 위반 상담의 폭언 메시지 원문
    @Query(value = """
            select a.message_id       as messageId,
                   m.created_at       as sentAt,
                   case when w.warning_type = 'MANUAL' then 'AGENT' else 'KEYWORD' end as detectedBy,
                   case when w.warning_id is null then null
                        else (select count(*) from warning w2
                               where w2.consultation_id = w.consultation_id
                                 and w2.warning_id <= w.warning_id) end as warningOrder,
                   a.original_content as originalContent,
                   m.content          as maskedContent
            from abuse_log a
            join message m on m.message_id = a.message_id
            left join warning w on w.abuse_log_id = a.abuse_log_id
            where a.consultation_id = :consultationId
            order by a.abuse_log_id
            """, nativeQuery = true)
    List<ViolationMessageRow> findOriginalMessages(@Param("consultationId") Long consultationId);
}
