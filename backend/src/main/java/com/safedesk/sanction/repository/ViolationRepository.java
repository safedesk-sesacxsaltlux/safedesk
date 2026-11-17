package com.safedesk.sanction.repository;

import com.safedesk.sanction.entity.Violation;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
