package com.safedesk.chat.service;

import com.safedesk.chat.dto.CancelResponseDTO;
import com.safedesk.chat.dto.ChatStartResponseDTO;
import com.safedesk.chat.dto.HandoffResponseDTO;
import com.safedesk.chat.dto.OpenChatDTO;
import com.safedesk.chat.dto.SanctionNoticeDTO;
import com.safedesk.chat.entity.CloseReason;
import com.safedesk.chat.entity.Consultation;
import com.safedesk.chat.entity.ConsultationStatus;
import com.safedesk.chat.repository.ConsultationRepository;
import com.safedesk.chat.repository.CustomerConsultationRepository;
import com.safedesk.common.event.ConsultationClosedEvent;
import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.sanction.entity.SanctionPolicy;
import com.safedesk.sanction.repository.SanctionPolicyRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class CustomerChatService {
    // 아직 끝나지 않은 상담. 이 상태의 상담이 있으면 새 상담을 만들지 않는다
    private static final List<String> OPEN_STATUSES =
        List.of(ConsultationStatus.AI_CHAT, ConsultationStatus.QUEUED, ConsultationStatus.IN_PROGRESS);
    private static final DateTimeFormatter RELEASE_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ConsultationRepository consultationRepository;
    private final CustomerConsultationRepository customerConsultationRepository;   // CUS-04 조건부 UPDATE
    private final ApplicationEventPublisher eventPublisher;                         // CUS-04 종료 이벤트 발행
    private final SanctionPolicyRepository sanctionPolicyRepository;               // CUS-01 새 상담에 넣을 정책

    // 생성자 주입
    public CustomerChatService(ConsultationRepository consultationRepository,
                               CustomerConsultationRepository customerConsultationRepository,
                               ApplicationEventPublisher eventPublisher,
                               SanctionPolicyRepository sanctionPolicyRepository) {
        this.consultationRepository = consultationRepository;
        this.customerConsultationRepository = customerConsultationRepository;
        this.eventPublisher = eventPublisher;
        this.sanctionPolicyRepository = sanctionPolicyRepository;
    }

    // CUS-01 상담 시작
    @Transactional
    public ChatStartResponseDTO start(long customerId) {
        // 같은 고객의 동시 요청을 줄 세운다. 회원이 없으면 토큰은 맞지만 탈퇴·삭제된 계정
        // ⚠️ 잠금은 반드시 이 트랜잭션의 첫 쿼리여야 한다. MySQL(REPEATABLE READ)은 첫 일반 조회 때 스냅샷을 고정하므로,
        //    잠금보다 먼저 조회가 있으면 뒤 요청이 앞 요청이 커밋한 상담을 못 보고 상담이 두 개 생긴다.
        customerConsultationRepository.lockCustomer(customerId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 정지 확인을 진행 중 상담 확인보다 먼저 한다.
        // 순서가 바뀌면 정지 고객이 정지 사실을 모른 채 기존 상담 화면으로 이동한다.
        LocalDateTime now = LocalDateTime.now();
        customerConsultationRepository.findActiveSanction(customerId, now)
            .ifPresent(sanction -> {
                throw new BusinessException(
                    ErrorCode.CUSTOMER_SANCTIONED,
                    ErrorCode.CUSTOMER_SANCTIONED.getMessage() + " "
                        + sanction.getReleaseAt().format(RELEASE_AT_FORMAT) + "에 해제됩니다.",
                    new SanctionNoticeDTO(toReason(sanction.getCloseReason()), sanction.getReleaseAt()));
            });

        // 기존 chatId 를 같이 준다. 없으면 고객이 닫을 수 없는 상담에 묶여 새 상담도 못 연다.
        customerConsultationRepository.findFirstByCustomerIdAndStatusIn(customerId, OPEN_STATUSES)
            .ifPresent(open -> {
                throw new BusinessException(
                    ErrorCode.CHAT_ALREADY_OPEN,
                    ErrorCode.CHAT_ALREADY_OPEN.getMessage(),
                    new OpenChatDTO(open.getConsultationId(), open.getStatus()));
            });

        // 시작 시점의 정책을 상담에 고정한다. 관리자가 정책을 바꿔도 진행 중 상담에는 적용하지 않는다 (체크리스트 S3)
        Long policyId = sanctionPolicyRepository.findFirstByOrderByPolicyIdDesc()
            .map(SanctionPolicy::getPolicyId)
            .orElse(null);

        // 첫 인사말은 저장하지 않는다. 아무 말 없이 나간 상담이 요약·통계에 잡히지 않게 (화면 고정 문구)
        Consultation saved = consultationRepository.save(Consultation.start(customerId, policyId));
        return new ChatStartResponseDTO(saved.getConsultationId(), saved.getStatus(), saved.getCreatedAt());
    }

    // 정지 안내 문구 (HIS-02 currentSanction.reason 과 같은 기준)
    private String toReason(String closeReason) {
        if (CloseReason.ABUSE_MANUAL.equals(closeReason)) {
            return "상담 중 폭언 (상담원 종료)";
        }
        return "상담 중 반복 폭언 (자동 종료)";
    }

    @Transactional
    public HandoffResponseDTO handoff(long chatId, long customerId) {
        // 락을 걸고 조회 -> 값이 있을 땐 꺼내고, 없을 때는 예외
        Consultation consultation = consultationRepository.findWithLockByConsultationId(chatId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_NOT_FOUND));

        // 본인 상담이 아니면 예외 처리
        if (consultation.getCustomerId() != customerId) {
            throw new BusinessException(ErrorCode.CHAT_ACCESS_DENIED);
        }

        // 상태 처리
        switch (consultation.getStatus()) {
            case ConsultationStatus.AI_CHAT -> consultation.handoff();
            case ConsultationStatus.QUEUED -> { }                // 아무것도 안 함
            case ConsultationStatus.IN_PROGRESS -> throw new BusinessException(ErrorCode.CHAT_ALREADY_ACTIVE);
            default -> throw new BusinessException(ErrorCode.CHAT_CLOSED);
        }
        return new HandoffResponseDTO(chatId, consultation.getStatus(), consultation.getQueuedAt());
    }

    // CUS-04 고객 상담 종료 (AI 응대·대기 중)
    @Transactional
    public CancelResponseDTO cancel(long chatId, long customerId) {
        // 락 없이 조회한다. 상태 변경은 아래 조건부 UPDATE 가 DB 에서 한 번에 처리한다.
        Consultation consultation = consultationRepository.findById(chatId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_NOT_FOUND));

        // 본인 상담이 아니면 예외 처리
        if (consultation.getCustomerId() != customerId) {
            throw new BusinessException(ErrorCode.CHAT_ACCESS_DENIED);
        }

        // 이미 끝난 상담이면 바꾸지 않고 현재 상태로 응답한다. (종료 버튼을 두 번 눌러도 오류가 아니게)
        if (ConsultationStatus.CLOSED.equals(consultation.getStatus())
            || ConsultationStatus.TERMINATED.equals(consultation.getStatus())) {
            return new CancelResponseDTO(chatId, consultation.getStatus(), consultation.getCloseReason(), consultation.getClosedAt());
        }

        // 종료 사유 정하기
        String closeReason = switch (consultation.getStatus()) {
            case ConsultationStatus.AI_CHAT -> CloseReason.NORMAL;        // AI 상담 중 종료
            case ConsultationStatus.QUEUED -> CloseReason.QUEUE_CANCEL;   // 대기 취소
            default -> throw new BusinessException(ErrorCode.CHAT_ALREADY_ACTIVE);
        };

        LocalDateTime now = LocalDateTime.now();
        int updated = customerConsultationRepository.closeByCustomer(chatId, closeReason, now);
        // 0 이면 조회 후 UPDATE 전에 상담원이 수락한 것 (체크리스트 E5)
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CHAT_ALREADY_ACTIVE);
        }

        // 같은 트랜잭션에서 발행해야 요약(INT-01)이 AFTER_COMMIT 으로 받고, 종료가 롤백되면 요약도 실행되지 않는다.
        eventPublisher.publishEvent(new ConsultationClosedEvent(chatId));

        return new CancelResponseDTO(chatId, ConsultationStatus.CLOSED, closeReason, now);
    }
}
