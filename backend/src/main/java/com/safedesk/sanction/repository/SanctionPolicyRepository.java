package com.safedesk.sanction.repository;

import com.safedesk.sanction.entity.SanctionPolicy;
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

    /**
     * 상담에 적용할 정책. 상담의 policy_id 정책을 쓰고, 비어 있거나 찾지 못하면 최신 정책으로 대신한다.
     * (정책을 바꿔도 새 상담부터 적용, REQ-024·체크리스트 S3. Consultation.policyId 가 엔티티에서 nullable 이라 대비)
     *
     * MOD-02(경고 기준), MOD-04(정지 일수)가 같이 쓴다.
     * default 메서드라 Spring Data 가 쿼리로 만들지 않는다. 단위 테스트에서 mock 으로 쓸 때는
     * when(repo.findByIdOrLatest(any())).thenCallRealMethod() 로 실제 로직을 타게 한다.
     *
     * @param policyId 상담에 저장된 정책 ID (null 가능)
     * @throws IllegalStateException 정책이 하나도 없을 때 (시드에 기본 정책이 항상 있으므로 DB 설정 오류)
     */
    default SanctionPolicy findByIdOrLatest(Long policyId) {
        Optional<SanctionPolicy> policy = (policyId != null) ? findById(policyId) : Optional.empty();

        return policy
            .or(this::findFirstByOrderByPolicyIdDesc)
            .orElseThrow(() -> new IllegalStateException("제재 정책이 없습니다."));
    }
}
