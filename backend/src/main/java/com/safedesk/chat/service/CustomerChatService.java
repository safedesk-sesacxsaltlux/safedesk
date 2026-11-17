package com.safedesk.chat.service;

import com.safedesk.chat.dto.CancelResponseDTO;
import com.safedesk.chat.dto.HandoffResponseDTO;
import com.safedesk.chat.entity.CloseReason;
import com.safedesk.chat.entity.Consultation;
import com.safedesk.chat.entity.ConsultationStatus;
import com.safedesk.chat.repository.ConsultationRepository;
import com.safedesk.chat.repository.CustomerConsultationRepository;
import com.safedesk.common.event.ConsultationClosedEvent;
import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CustomerChatService {
    private final ConsultationRepository consultationRepository;
    private final CustomerConsultationRepository customerConsultationRepository;   // CUS-04 조건부 UPDATE
    private final ApplicationEventPublisher eventPublisher;                         // CUS-04 종료 이벤트 발행

    // 생성자 주입
    public CustomerChatService(ConsultationRepository consultationRepository,
                               CustomerConsultationRepository customerConsultationRepository,
                               ApplicationEventPublisher eventPublisher) {
        this.consultationRepository = consultationRepository;
        this.customerConsultationRepository = customerConsultationRepository;
        this.eventPublisher = eventPublisher;
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
