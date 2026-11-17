package com.safedesk.sanction.service;

import com.safedesk.sanction.dto.SanctionResult;
import com.safedesk.sanction.entity.Sanction;
import com.safedesk.sanction.entity.SanctionPolicy;
import com.safedesk.sanction.entity.Violation;
import com.safedesk.sanction.repository.SanctionPolicyRepository;
import com.safedesk.sanction.repository.SanctionRepository;
import com.safedesk.sanction.repository.ViolationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 위반 기록 및 자동 이용정지 (MOD-04, 이슈 #54, docs/api/moderation.md).
 *
 * 이벤트 리스너가 아니라 일반 메서드다. 폭언 종료(MOD-03 자동, MOD-07 수동)가 「종료 트랜잭션 안에서」 호출한다.
 * 그래서 종료·위반·정지가 같이 성공하거나 같이 롤백된다 (종료됐는데 정지가 없는 상태 방지, NFR-009).
 *
 * @ Transactional 은 붙이지 않는다. 호출하는 쪽(ChatService.sendMessage() 등)의 트랜잭션에서 같이 실행된다.
 */
@Service
public class SanctionService {

    private final ViolationRepository violationRepository;
    private final SanctionRepository sanctionRepository;
    private final SanctionPolicyRepository sanctionPolicyRepository;

    public SanctionService(ViolationRepository violationRepository, SanctionRepository sanctionRepository, SanctionPolicyRepository sanctionPolicyRepository) {
        this.violationRepository = violationRepository;
        this.sanctionRepository = sanctionRepository;
        this.sanctionPolicyRepository = sanctionPolicyRepository;
    }

    /**
     * 폭언으로 종료된 상담에 위반을 기록하고, 누적 횟수에 맞게 이용정지한다.
     *
     * 인자로 Consultation 엔티티 대신 값을 받는 이유:
     * MOD-03 은 조건부 UPDATE(clearAutomatically)로 종료하므로 그 뒤의 Consultation 엔티티는 옛날 상태다.
     *
     * @param consultationId 종료된 상담
     * @param customerId     상담한 고객
     * @param policyId       상담에 저장된 정책 ID (null 이면 최신 정책)
     * @param closedAt       상담 종료 시각 = 정지 시작 시각 (REQ-021)
     * @return 위반·정지 결과. 같은 상담으로 다시 호출하면 기존 결과를 돌려준다
     */
    public SanctionResult recordViolationAndSuspend(
        long consultationId, long customerId, Long policyId, LocalDateTime closedAt) {

        // 1. 같은 상담의 위반이 이미 있으면 새로 만들지 않고 기존 결과를 돌려준다
        //   - 예외(DUPLICATE_VIOLATION)를 던지지 않는 이유: 같은 트랜잭션의 종료·메시지 저장까지 롤백된다
        //   - 동시에 두 번 들어오는 경우는 MOD-03 조건부 UPDATE 가 막고, 마지막으로 DB UNIQUE 가 막는다
        Optional<Violation> existing = violationRepository.findByConsultationId(consultationId);
        if (existing.isPresent()) {
            Violation violation = existing.get();
            Sanction sanction = sanctionRepository.findByViolationId(violation.getViolationId())
                .orElseThrow(() -> new IllegalStateException(
                    "위반에 연결된 정지가 없습니다. violationId =" + violation.getViolationId()
                ));
            long totalViolations = violationRepository.countByCustomerId(customerId);

            return SanctionResult.from(violation, sanction, totalViolations);
        }

        // 2. 위반 저장
        Violation violation = violationRepository.save(Violation.of(customerId, consultationId));

        // 3. 고객의 누적 위반 횟수 (방금 저장한 것 포함)
        long totalViolations = violationRepository.countByCustomerId(customerId);

        // 4. 정책 조회 → 정지 일수 결정 (상담 정책, 없으면 최신. MOD-02 와 같은 메서드)
        SanctionPolicy policy = sanctionPolicyRepository.findByIdOrLatest(policyId);
        int suspendDays = suspendDaysFor(policy, totalViolations);

        // 5. 정지 저장. 시작은 LocalDateTime.now() 가 아니라 closedAt (REQ-021, 테스트에서 시각을 고정하기도 쉽다)
        Sanction sanction = sanctionRepository.save(Sanction.of(violation.getViolationId(), customerId, suspendDays, closedAt));

        // 6. 결과 반환
        return SanctionResult.from(violation, sanction, totalViolations);
    }

    /**
     * 누적 횟수 → 정지 일수 (policy.md 5장).
     * 1회 → firstDays, 2회 → secondDays, 3회 이상 → thirdDays
     */
    private int suspendDaysFor(SanctionPolicy policy, long totalViolations) {
        if (totalViolations <= 1) {
            return policy.getFirstDays();
        }
        if (totalViolations == 2) {
            return policy.getSecondDays();
        }
        return policy.getThirdDays();
    }
}
