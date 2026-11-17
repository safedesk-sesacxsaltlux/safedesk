package com.safedesk.history.admin;

import com.safedesk.chat.entity.Consultation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AdminConsultationRepository extends Repository<Consultation, Long> {
    // ① "(:x is null or 조건)" 패턴: 파라미터가 null 이면 그 조건은 통과 → 선택 검색을 쿼리 하나로 처리
    // ② Consultation 에 Member 연관관계가 없어서 "join Member cu on cu.memberId = c.customerId" 로 id 를 직접 맞춘다
    //    상담원은 AI 단독 상담이면 없으니 left join (없으면 ag.name 이 null)
    // ③ 폭언 수는 메시지 테이블을 서브쿼리로 센다 (상담마다 count)
    // ④ 페이징이라 countQuery(전체 건수)를 따로 준다. where 가 같아야 totalElements 가 맞는다
    //    (count 에는 상담원 이름이 필요 없어서 left join Member ag 는 뺐다)

    @Query(value = """
            select new com.safedesk.history.admin.AdminConsultationRow(
                c.consultationId, c.createdAt, c.customerId, cu.loginId, c.agentId, ag.name, c.status,
                (select count(m) from Message m
                  where m.consultationId = c.consultationId and m.abuseStatus = 'ABUSIVE'))
            from Consultation c
            join Member cu on cu.memberId = c.customerId
            left join Member ag on ag.memberId = c.agentId
            where c.status in ('CLOSED', 'TERMINATED')
              and (:status is null or c.status = :status)
              and (:from is null or c.createdAt >= :from)
              and (:to is null or c.createdAt < :to)
              and (:agentHandled is null
                   or (:agentHandled = true and c.agentId is not null)
                   or (:agentHandled = false and c.agentId is null))
              and (:agentId is null or c.agentId = :agentId)
              and (:keywordLike is null or c.consultationId = :keywordId or cu.loginId like :keywordLike)
            order by c.createdAt desc, c.consultationId desc
            """,
            countQuery = """
            select count(c)
            from Consultation c
            join Member cu on cu.memberId = c.customerId
            where c.status in ('CLOSED', 'TERMINATED')
              and (:status is null or c.status = :status)
              and (:from is null or c.createdAt >= :from)
              and (:to is null or c.createdAt < :to)
              and (:agentHandled is null
                   or (:agentHandled = true and c.agentId is not null)
                   or (:agentHandled = false and c.agentId is null))
              and (:agentId is null or c.agentId = :agentId)
              and (:keywordLike is null or c.consultationId = :keywordId or cu.loginId like :keywordLike)
            """)
    Page<AdminConsultationRow> search(@Param("status") String status,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to,
                                      @Param("agentHandled") Boolean agentHandled,
                                      @Param("agentId") Long agentId,
                                      @Param("keywordId") Long keywordId,
                                      @Param("keywordLike") String keywordLike,
                                      Pageable pageable);
}
