package com.safedesk.history.agent;

import com.safedesk.chat.entity.Consultation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

// HIS-03 응대 고객별 이력 (읽기 전용)
// native SQL 을 쓴 이유:
//  ① "고객마다 최근 상담 1건"을 고르려면 ROW_NUMBER() 윈도 함수가 가장 간단하다 (MySQL 8)
//  ② violation 엔티티가 아직 dev 에 없다 (#57 머지 전)
public interface AgentCustomerRepository extends Repository<Consultation, Long> {

    // 안쪽 쿼리(x): 내가 맡은 종료 상담을 기간으로 거른 뒤, 고객별로
    //   rn  = 최신순 순번 (1 = 최근 상담),  cnt = 그 고객과 나의 상담 수
    // 바깥 쿼리: rn = 1 (고객당 한 줄)만 남기고, 최근 상태·키워드로 거른다
    // 키워드: 고객 아이디 부분 일치, 또는 그 상담번호가 이 고객과 나의 상담이면 그 고객
    String CUSTOMER_FROM = """
            from (
                select c.consultation_id, c.customer_id, c.created_at, c.inquiry_summary, c.status,
                       row_number() over (partition by c.customer_id
                                          order by c.created_at desc, c.consultation_id desc) as rn,
                       count(*) over (partition by c.customer_id) as cnt
                from consultation c
                where c.agent_id = :agentId
                  and c.status in ('CLOSED', 'TERMINATED')
                  and (:from is null or c.created_at >= :from)
                  and (:to is null or c.created_at < :to)
            ) x
            join member cu on cu.member_id = x.customer_id
            where x.rn = 1
              and (:status is null or x.status = :status)
              and (:keywordLike is null
                   or cu.login_id like :keywordLike
                   or exists (select 1 from consultation k
                               where k.customer_id = x.customer_id
                                 and k.agent_id = :agentId
                                 and k.consultation_id = :keywordId))
            """;

    @Query(value = """
            select x.customer_id as customerId,
                   cu.login_id as customerLoginId,
                   x.cnt as myConsultationCount,
                   exists (select 1 from violation v where v.customer_id = x.customer_id) as hasViolationHistory,
                   x.consultation_id as consultationId,
                   x.created_at as startedAt,
                   x.inquiry_summary as inquirySummary,
                   x.status as status,
                   (select count(*) from abuse_log a where a.consultation_id = x.consultation_id) as abuseCount
            """ + CUSTOMER_FROM + """
            order by x.created_at desc, x.consultation_id desc
            """,
        countQuery = "select count(*) " + CUSTOMER_FROM,
        nativeQuery = true)
    Page<AgentCustomerRow> searchCustomers(@Param("agentId") Long agentId,
                                           @Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to,
                                           @Param("status") String status,
                                           @Param("keywordId") Long keywordId,
                                           @Param("keywordLike") String keywordLike,
                                           Pageable pageable);

    // 상단 카드 4개. 기간 필터만 따른다 (상태·키워드와 무관)
    // 평균 상담 시간은 상담원이 수락한 시각부터 종료까지 (수락 시각이 없는 상담은 avg 에서 빠진다)
    @Query(value = """
            select count(distinct c.customer_id) as servedCustomerCount,
                   count(*) as totalConsultationCount,
                   avg(timestampdiff(second, c.accepted_at, c.closed_at)) as avgDurationSeconds,
                   sum(case when c.status = 'TERMINATED' then 1 else 0 end) as abuseTerminatedCount
            from consultation c
            where c.agent_id = :agentId
              and c.status in ('CLOSED', 'TERMINATED')
              and (:from is null or c.created_at >= :from)
              and (:to is null or c.created_at < :to)
            """, nativeQuery = true)
    AgentCustomerStatsRow stats(@Param("agentId") Long agentId,
                                @Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to);
}

