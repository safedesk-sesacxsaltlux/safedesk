package com.safedesk.sanction.repository;

import com.safedesk.sanction.entity.Sanction;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * sanction 저장소 (MOD-04).
 */
public interface SanctionRepository extends JpaRepository<Sanction, Long> {

    /**
     * 위반에 연결된 정지 (이미 위반이 있을 때 기존 결과를 돌려주는 용도).
     */
    Optional<Sanction> findByViolationId(Long violationId);

    // 참고: 「지금 정지 중인지」 조회(CUS-01 상담 시작, HIS-02 마이페이지)는 이 이슈 범위가 아니다

    // API-ADM-06 해제할 제재 잠금 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Sanction> findWithLockBySanctionId(Long sanctionId);

    // API-ADM-04 제재 목록. 조건이 null 이면 그 조건은 걸지 않는다.
    // 상태는 컬럼이 없어서 SanctionStatus.of() 와 같은 규칙을 SQL 로 건다 (페이지 수가 맞도록 DB 에서 거른다)
    @Query(value = """
            select s.sanction_id  as sanctionId,
                   s.customer_id  as customerId,
                   m.login_id     as loginId,
                   m.name         as name,
                   (select count(*) from violation v2
                     where v2.customer_id = s.customer_id
                       and v2.violation_id <= s.violation_id) as violationCount,
                   v.created_at   as violatedAt,
                   s.suspend_days as suspendDays,
                   s.start_at     as startAt,
                   s.end_at       as endAt,
                   s.released_at  as releasedAt
            from sanction s
            join member m on m.member_id = s.customer_id
            join violation v on v.violation_id = s.violation_id
            where (:keyword is null or m.login_id like concat('%', :keyword, '%')
                                    or m.name like concat('%', :keyword, '%'))
              and (:durationDays is null or s.suspend_days = :durationDays)
              and (:status is null
                   or (:status = 'RELEASED'  and s.released_at is not null)
                   or (:status = 'SUSPENDED' and s.released_at is null and s.end_at > :now)
                   or (:status = 'EXPIRED'   and s.released_at is null and s.end_at <= :now))
            order by s.created_at desc, s.sanction_id desc
            """,
        countQuery = """
            select count(*)
            from sanction s
            join member m on m.member_id = s.customer_id
            where (:keyword is null or m.login_id like concat('%', :keyword, '%')
                                    or m.name like concat('%', :keyword, '%'))
              and (:durationDays is null or s.suspend_days = :durationDays)
              and (:status is null
                   or (:status = 'RELEASED'  and s.released_at is not null)
                   or (:status = 'SUSPENDED' and s.released_at is null and s.end_at > :now)
                   or (:status = 'EXPIRED'   and s.released_at is null and s.end_at <= :now))
            """,
        nativeQuery = true)
    Page<SanctionListRow> findList(@Param("keyword") String keyword,
                                   @Param("status") String status,
                                   @Param("durationDays") Integer durationDays,
                                   @Param("now") LocalDateTime now,
                                   Pageable pageable);
}
