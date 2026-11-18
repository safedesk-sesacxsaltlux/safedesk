package com.safedesk.dashboard;

import com.safedesk.chat.entity.Consultation;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

// HIS-07 운영 대시보드 집계 (읽기 전용)
// 여러 테이블(consultation, abuse_log, sanction, violation)을 집계만 해서 native SQL 로 쓴다.
// 기간은 [from, to) : from 은 시작일 0시, to 는 마지막 날 다음 날 0시 (마지막 날 하루 포함)
public interface DashboardRepository extends Repository<Consultation, Long> {
    // 요약 카드 5개. 지표마다 기준 시각이 달라서(명세 「집계 기준」) 서브쿼리로 따로 센다
    @Query(value = """
            select
              (select count(*) from consultation
                where created_at >= :from and created_at < :to) as totalConsultationCount,
              (select count(*) from abuse_log
                where created_at >= :from and created_at < :to) as abuseDetectedCount,
              (select count(*) from consultation
                where status = 'TERMINATED' and closed_by = 'SYSTEM'
                  and closed_at >= :from and closed_at < :to) as autoTerminatedCount,
              (select count(*) from sanction
                where created_at >= :from and created_at < :to) as newSanctionCount,
              (select count(distinct customer_id) from sanction
                where released_at is null and end_at > :now) as currentSuspendedCustomerCount
            """, nativeQuery = true)
    DashboardRows.CardRow cards(@Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to,
                                @Param("now") LocalDateTime now);
    // 종료 유형 (기준: 종료 시각). AI 단독 해결률도 여기서 계산한다
    @Query(value = """
            select count(*) as totalClosedCount,
                   sum(case when status = 'CLOSED' and queued_at is null then 1 else 0 end) as aiClosedCount,
                   sum(case when status = 'CLOSED' and accepted_at is not null then 1 else 0 end) as agentClosedCount,
                   sum(case when status = 'CLOSED' and queued_at is not null and accepted_at is null
                            then 1 else 0 end) as queueCanceledCount,
                   sum(case when status = 'TERMINATED' then 1 else 0 end) as terminatedCount
            from consultation
            where status in ('CLOSED', 'TERMINATED')
              and closed_at >= :from and closed_at < :to
            """, nativeQuery = true)
    DashboardRows.CloseTypeRow closeTypes(@Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);
    // 일별 시작된 상담 수. 건수가 있는 날만 나오므로 0 인 날은 서비스에서 채운다
    @Query(value = """
            select date_format(created_at, '%Y-%m-%d') as day, count(*) as cnt
            from consultation
            where created_at >= :from and created_at < :to
            group by date_format(created_at, '%Y-%m-%d')
            """, nativeQuery = true)
    List<DashboardRows.DailyCountRow> dailyConsultations(@Param("from") LocalDateTime from,
                                                         @Param("to") LocalDateTime to);

    // 일별 폭언 감지 수 (abuse_log 기준)
    @Query(value = """
            select date_format(created_at, '%Y-%m-%d') as day, count(*) as cnt
            from abuse_log
            where created_at >= :from and created_at < :to
            group by date_format(created_at, '%Y-%m-%d')
            """, nativeQuery = true)
    List<DashboardRows.DailyCountRow> dailyAbuse(@Param("from") LocalDateTime from,
                                                 @Param("to") LocalDateTime to);
    // 최근 생긴 제재 3건 (기간과 무관)
    // violationCount: 그 고객의 위반 중 이 제재의 위반까지 몇 번째인지 (정지 일수가 정해진 근거)
    @Query(value = """
            select s.sanction_id as sanctionId,
                   s.customer_id as customerId,
                   m.login_id as customerLoginId,
                   (select count(*) from violation v
                     where v.customer_id = s.customer_id and v.violation_id <= s.violation_id) as violationCount,
                   s.suspend_days as suspendDays,
                   s.end_at as endAt,
                   s.released_at as releasedAt
            from sanction s
            join member m on m.member_id = s.customer_id
            order by s.created_at desc, s.sanction_id desc
            limit 3
            """, nativeQuery = true)
    List<DashboardRows.RecentSanctionRow> recentSanctions();
}

