package com.safedesk.moderation.service;

import com.safedesk.chat.entity.Consultation;
import com.safedesk.chat.entity.ConsultationStatus;
import com.safedesk.chat.repository.ConsultationRepository;
import com.safedesk.common.event.ConsultationClosedEvent;
import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.moderation.dto.AbuseFilterResult;
import com.safedesk.moderation.dto.TerminationResult;
import com.safedesk.moderation.dto.WarningResult;
import com.safedesk.moderation.entity.AbuseLog;
import com.safedesk.moderation.entity.Warning;
import com.safedesk.moderation.filter.AbuseFilter;
import com.safedesk.moderation.repository.AbuseLogRepository;
import com.safedesk.moderation.repository.WarningRepository;
import com.safedesk.sanction.dto.SanctionResult;
import com.safedesk.sanction.entity.SanctionPolicy;
import com.safedesk.sanction.repository.SanctionPolicyRepository;
import com.safedesk.sanction.service.SanctionService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 폭언 판정·원문 저장·자동 경고·자동 종료 (MOD-01, MOD-02, MOD-03, docs/api/moderation.md).
 *
 * 메서드를 나눈 이유: abuse_log 에는 message_id 가 필요한데, 이 값은 메시지를 저장한 뒤에야 생긴다.
 * 그래서 ChatService 가 「① 판정 → 메시지 저장 → ② 원문 저장 → ③ 자동 경고 → ④ 자동 종료」 순서로 호출한다.
 *
 * @ Transactional 은 붙이지 않는다. ChatService.sendMessage() 의 트랜잭션 안에서 같이 실행된다.
 * 그래서 경고·종료·제재 저장이 실패하면 메시지·원문 저장까지 같이 롤백된다.
 */
@Service
public class ModerationService {

    private final AbuseFilter abuseFilter;
    private final AbuseLogRepository abuseLogRepository;
    private final WarningRepository warningRepository;
    private final SanctionPolicyRepository sanctionPolicyRepository;
    private final ConsultationRepository consultationRepository;
    private final SanctionService sanctionService;
    private final ApplicationEventPublisher eventPublisher;

    public ModerationService(
        AbuseFilter abuseFilter,
        AbuseLogRepository abuseLogRepository,
        WarningRepository warningRepository,
        SanctionPolicyRepository sanctionPolicyRepository,
        ConsultationRepository consultationRepository,
        SanctionService sanctionService,
        ApplicationEventPublisher eventPublisher) {
        this.abuseFilter = abuseFilter;
        this.abuseLogRepository = abuseLogRepository;
        this.warningRepository = warningRepository;
        this.sanctionPolicyRepository = sanctionPolicyRepository;
        this.consultationRepository = consultationRepository;
        this.sanctionService = sanctionService;
        this.eventPublisher = eventPublisher;
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
        SanctionPolicy policy = sanctionPolicyRepository.findByIdOrLatest(consultation.getPolicyId());

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
     * ④ 폭언 누적 자동 종료 (MOD-03). ChatService 가 applyAutoWarning() 결과의 shouldTerminate 가 true 일 때 호출한다.
     *
     * 경고 횟수를 다시 세지 않는다. MOD-02 의 shouldTerminate(AUTO + MANUAL, 상담 정책 기준)를 그대로 믿는다.
     * 반환이 Optional 인 이유: 이미 종료된 상담이면 예외 없이 알려주려고 (예외를 던지면 메시지 저장까지 롤백된다).
     *
     * @return 종료했으면 결과(정지 일수 포함), 이미 종료된 상담이면 Optional.empty()
     */
    public Optional<TerminationResult> terminateForAbuse(long consultationId) {

        // 1. 상담 조회 → 고객 ID·정책 ID 를 꺼내 둔다
        //    2번의 UPDATE 가 영속성 컨텍스트를 비우므로(clearAutomatically) 그 「전」에 꺼내야 한다
        Consultation consultation = consultationRepository.findById(consultationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_NOT_FOUND));
        long customerId = consultation.getCustomerId();
        Long policyId = consultation.getPolicyId();

        // 2. 조건부 UPDATE 로 종료. 종료 시각을 한 번만 만들어 UPDATE 와 MOD-04 에 같은 값을 넘긴다
        //    0 행이면 이미 종료됐거나 상담원이 먼저 종료한 것 → 위반·이벤트 모두 만들지 않는다
        LocalDateTime closedAt = LocalDateTime.now();
        if (consultationRepository.terminateForAbuse(consultationId, closedAt) == 0) {
            return Optional.empty();
        }

        // 3. 위반 기록·자동 정지 (MOD-04)
        //    예외를 잡지 않는다. 실패하면 종료·메시지·경고까지 같이 롤백된다 (NFR-009)
        SanctionResult sanctionResult =
            sanctionService.recordViolationAndSuspend(consultationId, customerId, policyId, closedAt);

        // 4. 상담 요약 이벤트 (INT-01). 수신 쪽이 AFTER_COMMIT 이라 롤백되면 요약도 실행되지 않는다
        eventPublisher.publishEvent(new ConsultationClosedEvent(consultationId));

        // 5. 결과 반환. ChatService 가 이 값으로 종료 알림을 보낸다
        return Optional.of(TerminationResult.of(consultationId, closedAt, sanctionResult));
    }
}
