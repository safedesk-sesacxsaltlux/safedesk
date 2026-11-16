package com.safedesk.moderation.service;

import com.safedesk.chat.entity.Consultation;
import com.safedesk.chat.entity.ConsultationStatus;
import com.safedesk.chat.repository.ConsultationRepository;
import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.moderation.dto.AbuseFilterResult;
import com.safedesk.moderation.dto.WarningResult;
import com.safedesk.moderation.entity.AbuseLog;
import com.safedesk.moderation.entity.Warning;
import com.safedesk.moderation.filter.AbuseFilter;
import com.safedesk.moderation.repository.AbuseLogRepository;
import com.safedesk.moderation.repository.WarningRepository;
import com.safedesk.sanction.entity.SanctionPolicy;
import com.safedesk.sanction.repository.SanctionPolicyRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 폭언 판정·원문 저장·자동 경고 (MOD-01, MOD-02, docs/api/moderation.md).
 *
 * 메서드를 나눈 이유: abuse_log 에는 message_id 가 필요한데, 이 값은 메시지를 저장한 뒤에야 생긴다.
 * 그래서 ChatService 가 「① 판정 → 메시지 저장 → ② 원문 저장 → ③ 자동 경고」 순서로 호출한다.
 *
 * @ Transactional 은 붙이지 않는다. ChatService.sendMessage() 의 트랜잭션 안에서 같이 실행된다.
 * 그래서 경고 저장이 실패하면 메시지·원문 저장까지 같이 롤백된다.
 */
@Service
public class ModerationService {

    private final AbuseFilter abuseFilter;
    private final AbuseLogRepository abuseLogRepository;
    private final WarningRepository warningRepository;
    private final SanctionPolicyRepository sanctionPolicyRepository;
    private final ConsultationRepository consultationRepository;

    public ModerationService(
        AbuseFilter abuseFilter,
        AbuseLogRepository abuseLogRepository,
        WarningRepository warningRepository,
        SanctionPolicyRepository sanctionPolicyRepository,
        ConsultationRepository consultationRepository) {
        this.abuseFilter = abuseFilter;
        this.abuseLogRepository = abuseLogRepository;
        this.warningRepository = warningRepository;
        this.sanctionPolicyRepository = sanctionPolicyRepository;
        this.consultationRepository = consultationRepository;
    }

    /**
     * ① 판정만 한다. ChatService 가 메시지를 저장하기 「전」에 호출한다.
     *
     * @return NORMAL 이면 원문 그대로, ABUSIVE 이면 금칙어만 *** 로 가린 문장
     */
    public AbuseFilterResult processMessage(String content) {
        return abuseFilter.filter(content);
    }

    /**
     * ② 원문 저장. ChatService 가 메시지를 저장한 「뒤」, ABUSIVE 일 때만 호출한다.
     *
     * @return 저장된 AbuseLog (MOD-02 에서 경고에 abuse_log_id 를 연결할 때 쓴다)
     */
    public AbuseLog saveOriginal(long messageId, long consultationId, String originalContent) {
        return abuseLogRepository.save(new AbuseLog(messageId, consultationId, originalContent));
    }

    /**
     * ③ 자동 경고 (MOD-02). ChatService 가 saveOriginal() 「뒤」에 호출한다.
     *
     * 반환이 Optional 인 이유: 경고를 만들지 않은 경우(중복·종료된 상담)를 예외 없이 알려주려고.
     * 예외를 던지면 같은 트랜잭션의 메시지 저장까지 롤백된다.
     *
     * @return 경고를 저장했으면 결과, 경고를 만들지 않았으면 Optional.empty()
     */
    public Optional<WarningResult> applyAutoWarning(long consultationId, long abuseLogId) {

        // 1. 상담 조회. ChatService 가 같은 트랜잭션에서 이미 조회했으므로 DB 를 다시 읽지 않고 캐시에서 가져온다
        Consultation consultation = consultationRepository.findById(consultationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_NOT_FOUND));

        // 2. 상담원과 상담 중일 때만 경고한다 (종료된 상담에는 경고하지 않음)
        //    ChatService 에서 이미 확인하지만, MOD-06 등 다른 곳에서도 부를 수 있어서 한 번 더 본다
        if (!ConsultationStatus.IN_PROGRESS.equals(consultation.getStatus())) {
            return Optional.empty();
        }

        // 3. 같은 폭언 기록으로 경고가 이미 있으면 다시 만들지 않는다
        if (warningRepository.existsByAbuseLogId(abuseLogId)) {
            return Optional.empty();
        }

        // 4. 상담에 적용된 정책 (정책을 바꿔도 새 상담부터 적용, 체크리스트 S3)
        SanctionPolicy policy = findPolicy(consultation);

        // 5. 경고 저장
        Warning saved = warningRepository.save(Warning.auto(consultationId, abuseLogId));

        // 6. 이 상담의 경고 횟수 (방금 저장한 경고 포함, AUTO + MANUAL)
        long warningCount = warningRepository.countByConsultationId(consultationId);

        // 7. 종료 기준에 도달했는지 함께 돌려준다. 실제 종료는 MOD-03
        int closeThreshold = policy.getCloseThreshold();

        return Optional.of(new WarningResult(
            consultationId,
            saved.getWarningId(),
            warningCount,
            closeThreshold,
            warningCount >= closeThreshold
        ));
    }

    /**
     * 상담에 저장된 policy_id 의 정책. 비어 있거나 찾지 못하면 최신 정책으로 대신한다.
     * (Consultation.policyId 가 엔티티에서 nullable 이라 대비)
     */
    private SanctionPolicy findPolicy(Consultation consultation) {
        Long policyId = consultation.getPolicyId();

        Optional<SanctionPolicy> policy = (policyId != null)
            ? sanctionPolicyRepository.findById(policyId)
            : Optional.empty();

        return policy
            .or(sanctionPolicyRepository::findFirstByOrderByPolicyIdDesc)
            // 시드에 기본 정책이 항상 있으므로, 여기까지 오면 DB 설정 오류다
            .orElseThrow(() -> new IllegalStateException("제재 정책이 없습니다."));
    }
}
