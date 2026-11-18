package com.safedesk.history.customer;

import com.safedesk.chat.entity.Consultation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

// HIS-01·02 고객 본인 이력·이용 상태 (읽기 전용)
// 이름을 CustomerHistoryRepository 로 한 이유: chat 패키지에 CustomerConsultationRepository(CUS) 가 이미 있어서
//  같은 이름이면 Spring 빈 이름이 겹친다.
public interface CustomerHistoryRepository extends Repository<Consultation, Long> {

    // HIS-01 내 종료 상담. 위반 여부는 Violation 엔티티를 서브쿼리로 센다 (상담 1건 = 위반 0 또는 1)
    // agentHandled: null 이면 전체, false 면 AI 단독(상담원 없음), true 면 상담원 연결
    @Query(value = """
            select new com.safedesk.history.customer.CustomerConsultationRow(
                c.consultationId, c.createdAt, c.closedAt, c.agentId, c.status,
                c.inquirySummary, c.inquiryCategory, c.processSummary, c.result,
                (select count(v) from Violation v where v.consultationId = c.consultationId))
            from Consultation c
            where c.customerId = :customerId
              and c.status in ('CLOSED', 'TERMINATED')
              and (:agentHandled is null
                   or (:agentHandled = true and c.agentId is not null)
                   or (:agentHandled = false and c.agentId is null))
            order by c.createdAt desc, c.consultationId desc
            """,
            countQuery = """
            select count(c)
            from Consultation c
            where c.customerId = :customerId
              and c.status in ('CLOSED', 'TERMINATED')
              and (:agentHandled is null
                   or (:agentHandled = true and c.agentId is not null)
                   or (:agentHandled = false and c.agentId is null))
            """)
    Page<CustomerConsultationRow> searchMyConsultations(@Param("customerId") Long customerId,
                                                        @Param("agentHandled") Boolean agentHandled,
                                                        Pageable pageable);

    // HIS-02 지금 정지 중인 제재. 기준은 상담 시작(CUS-01)과 같다: 수동 해제 안 됨 + 해제 일시가 아직 안 지남
    // 사유를 만들려고 위반 상담의 종료 사유도 같이 가져온다
    @Query(value = """
            select s.sanction_id as sanctionId, s.suspend_days as suspendDays,
                   s.start_at as startAt, s.end_at as endAt, c.close_reason as closeReason
            from sanction s
            join violation v on v.violation_id = s.violation_id
            join consultation c on c.consultation_id = v.consultation_id
            where s.customer_id = :customerId
              and s.released_at is null
              and s.end_at > :now
            order by s.end_at desc
            limit 1
            """, nativeQuery = true)
    Optional<ActiveSanctionInfo> findActiveSanction(@Param("customerId") Long customerId,
                                                    @Param("now") LocalDateTime now);
}
