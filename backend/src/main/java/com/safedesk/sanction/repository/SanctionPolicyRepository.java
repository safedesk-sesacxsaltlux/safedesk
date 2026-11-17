package com.safedesk.sanction.repository;

import com.safedesk.sanction.entity.SanctionPolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * sanction_policy 저장소.
 *
 * 상담의 policy_id 로 찾을 때는 기본 제공 findById() 를 쓴다.
 */
public interface SanctionPolicyRepository extends JpaRepository<SanctionPolicy, Long> {

    /**
     * 가장 최근에 저장된 정책 (= 현재 적용 중인 정책).
     * 정책은 수정하지 않고 새 행을 추가하므로 policy_id 가 가장 큰 행이 최신이다.
     * SELECT * FROM sanction_policy ORDER BY policy_id DESC LIMIT 1
     *
     * MOD-02: 상담에 policy_id 가 비어 있을 때 대신 쓴다 (Consultation.policyId 가 엔티티에서 nullable).
     * 상담 시작(CUS-01)에서도 새 상담에 넣을 정책을 고를 때 쓸 수 있다.
     */
    Optional<SanctionPolicy> findFirstByOrderByPolicyIdDesc();

    Page<SanctionPolicy> findAllByOrderByPolicyIdDesc(Pageable pageable);

    Optional<SanctionPolicy> findFirstByPolicyIdLessThanOrderByPolicyIdDesc(Long policyId);
}
