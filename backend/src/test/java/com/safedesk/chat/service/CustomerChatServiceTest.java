package com.safedesk.chat.service;

import com.safedesk.chat.dto.HandoffResponseDTO;
import com.safedesk.chat.entity.CloseReason;
import com.safedesk.chat.entity.ClosedBy;
import com.safedesk.chat.entity.Consultation;
import com.safedesk.chat.entity.ConsultationStatus;
import com.safedesk.chat.repository.ConsultationRepository;
import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerChatServiceTest {

    private static final long CHAT_ID = 1L;
    private static final long CUSTOMER_ID = 100L;

    private ConsultationRepository consultationRepository;
    private CustomerChatService customerChatService;

    @BeforeEach   // 각 테스트 전에 매번 새로 준비 (테스트끼리 영향 안 주게)
    void setUp() {
        consultationRepository = mock(ConsultationRepository.class);
        customerChatService = new CustomerChatService(consultationRepository);
    }

    // 조회하면 이 상담이 나오도록 가짜 리포지토리에 등록
    private void givenConsultation(Consultation consultation) {
        when(consultationRepository.findWithLockByConsultationId(CHAT_ID))
            .thenReturn(Optional.of(consultation));
    }

    @Test
    @DisplayName("AI 상담 중이면 QUEUED로 바꾸고 queuedAt을 기록한다")
    void handoffWhenAiChat() {
        // 준비
        Consultation consultation = Consultation.start(CUSTOMER_ID, 1L);
        givenConsultation(consultation);

        // 실행
        HandoffResponseDTO response = customerChatService.handoff(CHAT_ID, CUSTOMER_ID);

        // 확인
        assertThat(response.status()).isEqualTo(ConsultationStatus.QUEUED);
        assertThat(response.queuedAt()).isNotNull();
    }

    @Test
    @DisplayName("다른 고객의 상담이면 CHAT_ACCESS_DENIED")
    void handoffWhenNotOwner() {
        givenConsultation(Consultation.start(999L, 1L));   // 주인은 999번 고객

        assertThatThrownBy(() -> customerChatService.handoff(CHAT_ID, CUSTOMER_ID))   // 100번이 요청
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.CHAT_ACCESS_DENIED);
    }

    // 이미 QUEUED
    @Test
    @DisplayName("이미 대기 중이면 그대로 두고 queuedAt을 유지")
    void handoffWhenAlreadyQueued() {
        // 준비
        Consultation consultation = Consultation.start(CUSTOMER_ID, 1L);
        givenConsultation(consultation);

        // 실행
        HandoffResponseDTO response = customerChatService.handoff(CHAT_ID, CUSTOMER_ID);
        HandoffResponseDTO response2 = customerChatService.handoff(CHAT_ID, CUSTOMER_ID);

        // 확인
        assertThat(response2.status()).isEqualTo(ConsultationStatus.QUEUED);
        assertThat(response2.queuedAt()).isEqualTo(response.queuedAt());
    }

    // 상담원과 연결됨
    @Test
    @DisplayName("상담원과 연결된 상담이면 CHAT_ALREADY_ACTIVE")
    void handoffWhenInProgress() {
        // 준비: accept()는 QUEUED에서만 되므로 실제 흐름대로 AI_CHAT → QUEUED → IN_PROGRESS
        Consultation consultation = Consultation.start(CUSTOMER_ID, 1L);
        consultation.handoff();
        consultation.accept(10L);
        givenConsultation(consultation);

        // 실행 + 확인
        assertThatThrownBy(() -> customerChatService.handoff(CHAT_ID, CUSTOMER_ID))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.CHAT_ALREADY_ACTIVE);
    }

    // 종료된 상담
    @Test
    @DisplayName("종료된 상담이면 CHAT_CLOSED")
    void handoffWhenClosed() {
        // 준비: AI 상담 중 고객이 종료한 상담
        Consultation consultation = Consultation.start(CUSTOMER_ID, 1L);
        consultation.close(ClosedBy.CUSTOMER, CloseReason.NORMAL);
        givenConsultation(consultation);

        // 실행 + 확인
        assertThatThrownBy(() -> customerChatService.handoff(CHAT_ID, CUSTOMER_ID))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.CHAT_CLOSED);
    }

    // 없는 상담
    @Test
    @DisplayName("없는 상담이면 CHAT_NOT_FOUND")
    void handoffWhenNotFound() {
        // 준비: 조회하면 빈 값이 나오도록
        when(consultationRepository.findWithLockByConsultationId(CHAT_ID))
            .thenReturn(Optional.empty());

        // 실행 + 확인
        assertThatThrownBy(() -> customerChatService.handoff(CHAT_ID, CUSTOMER_ID))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.CHAT_NOT_FOUND);
    }
}
