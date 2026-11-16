package com.safedesk.moderation.repository;

import com.safedesk.moderation.entity.Warning;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * warning 저장소.
 *
 * 메서드 이름으로 쿼리를 만든다 (Spring Data JPA). 이름의 필드명은 Warning 엔티티의 필드명과 같아야 한다.
 */
public interface WarningRepository extends JpaRepository<Warning, Long> {

    /**
     * 같은 폭언 기록으로 경고가 이미 있는지 (중복 경고 방지, MOD-02 처리 순서 3번).
     * SELECT 1 FROM warning WHERE abuse_log_id = ? LIMIT 1
     */
    boolean existsByAbuseLogId(Long abuseLogId);

    /**
     * 이 상담의 경고 횟수. AUTO + MANUAL 모두 센다 (정책 4장 「수동 경고도 1회로 셈」).
     * 상담 단위로 세므로 새 상담은 0회부터 시작한다.
     * SELECT COUNT(*) FROM warning WHERE consultation_id = ?
     */
    long countByConsultationId(Long consultationId);
}
