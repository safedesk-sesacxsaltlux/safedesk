package com.safedesk.sanction.repository;

import com.safedesk.sanction.entity.Sanction;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
