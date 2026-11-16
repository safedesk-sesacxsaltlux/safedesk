package com.safedesk.chat.service;

import com.safedesk.chat.entity.Consultation;
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
    public HandoffResponse handoff(long chatId, long customerId) {
        // 락을 걸고 조회 -> 값이 있을 땐 꺼내고, 없을 때는 예외를 던진다
        Consultation consultation = consultationRepository.findWithLockByConsultationId(chatId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_NOT_FOUND));


    }
}
