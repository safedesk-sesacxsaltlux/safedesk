package com.safedesk.history.agent;

import com.safedesk.chat.entity.Consultation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AgentConsultationRepository extends Repository<Consultation, Long> {
    // HIS-05 와 같은 구조. 차이점은 "c.agentId = :agentId" 가 항상 걸린다는 것 (다른 상담원 상담은 안 나옴)
    // 고객 아이디가 필요해서 Member 를 join, 폭언 수는 서브쿼리
    // countQuery 에는 고객 아이디가 필요 없어서 join 을 뺐다 (조건에 고객 아이디를 쓰지 않으므로 건수는 같다)
    @Query(value = """
            select new com.safedesk.history.agent.AgentConsultationRow(
                c.consultationId, cu.loginId, c.createdAt, c.closedAt, c.inquirySummary, c.status,
                (select count(m) from Message m
                  where m.consultationId = c.consultationId and m.abuseStatus = 'ABUSIVE'))
            from Consultation c
            join Member cu on cu.memberId = c.customerId
            where c.agentId = :agentId
              and c.status in ('CLOSED', 'TERMINATED')
              and (:customerId is null or c.customerId = :customerId)
              and (:status is null or c.status = :status)
              and (:from is null or c.createdAt >= :from)
              and (:to is null or c.createdAt < :to)
            order by c.createdAt desc, c.consultationId desc
            """,
        countQuery = """
            select count(c)
            from Consultation c
            where c.agentId = :agentId
              and c.status in ('CLOSED', 'TERMINATED')
              and (:customerId is null or c.customerId = :customerId)
              and (:status is null or c.status = :status)
              and (:from is null or c.createdAt >= :from)
              and (:to is null or c.createdAt < :to)
            """)
    Page<AgentConsultationRow> search(@Param("agentId") Long agentId,
                                      @Param("customerId") Long customerId,
                                      @Param("status") String status,
                                      @Param("from")LocalDateTime from,
                                      @Param("to") LocalDateTime to,
                                      Pageable pageable);
}
