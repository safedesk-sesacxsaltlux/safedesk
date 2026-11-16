package com.safedesk.chat.service;

import com.safedesk.chat.dto.HandoffResponseDTO;
import com.safedesk.chat.entity.Consultation;
import com.safedesk.chat.entity.ConsultationStatus;
import com.safedesk.chat.repository.ConsultationRepository;
import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerChatService {
    private final ConsultationRepository consultationRepository;

    // 생성자 주입
    public CustomerChatService(ConsultationRepository consultationRepository) {
        this.consultationRepository = consultationRepository;
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
}
