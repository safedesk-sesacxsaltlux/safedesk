package com.safedesk.chat.service;

import com.safedesk.chat.dto.CancelResponseDTO;
import com.safedesk.chat.dto.ChatStartResponseDTO;
import com.safedesk.chat.dto.HandoffResponseDTO;
import com.safedesk.chat.dto.OpenChatDTO;
import com.safedesk.chat.dto.SanctionNoticeDTO;
import com.safedesk.chat.entity.CloseReason;
import com.safedesk.chat.entity.ClosedBy;
import com.safedesk.chat.entity.Consultation;
import com.safedesk.chat.entity.ConsultationStatus;
import com.safedesk.chat.repository.ActiveSanctionRow;
import com.safedesk.chat.repository.ConsultationRepository;
import com.safedesk.chat.repository.CustomerConsultationRepository;
import com.safedesk.common.event.ConsultationClosedEvent;
import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.sanction.entity.SanctionPolicy;
import com.safedesk.sanction.repository.SanctionPolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerChatServiceTest {

    private static final long CHAT_ID = 1L;
    private static final long CUSTOMER_ID = 100L;

    private ConsultationRepository consultationRepository;
    private CustomerConsultationRepository customerConsultationRepository;
    private ApplicationEventPublisher eventPublisher;
    private SanctionPolicyRepository sanctionPolicyRepository;
    private CustomerChatService customerChatService;

    @BeforeEach   // 각 테스트 전에 매번 새로 준비 (테스트끼리 영향 안 주게)
    void setUp() {
        consultationRepository = mock(ConsultationRepository.class);
        customerConsultationRepository = mock(CustomerConsultationRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        sanctionPolicyRepository = mock(SanctionPolicyRepository.class);
        customerChatService = new CustomerChatService(
            consultationRepository, customerConsultationRepository, eventPublisher, sanctionPolicyRepository);
    }

    // ===== CUS-01 상담 시작 =====

    // 정지 없음, 진행 중 상담 없음, 최신 정책 7번인 고객
    private void givenCanStart() {
        when(customerConsultationRepository.lockCustomer(CUSTOMER_ID)).thenReturn(Optional.of(CUSTOMER_ID));
        when(customerConsultationRepository.findActiveSanction(eq(CUSTOMER_ID), any())).thenReturn(Optional.empty());
        when(customerConsultationRepository.findFirstByCustomerIdAndStatusIn(eq(CUSTOMER_ID), any()))
            .thenReturn(Optional.empty());
        SanctionPolicy policy = mock(SanctionPolicy.class);
        when(policy.getPolicyId()).thenReturn(7L);
        when(sanctionPolicyRepository.findFirstByOrderByPolicyIdDesc()).thenReturn(Optional.of(policy));
        when(consultationRepository.save(any(Consultation.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private ActiveSanctionRow sanctionRow(LocalDateTime releaseAt, String closeReason) {
        ActiveSanctionRow row = mock(ActiveSanctionRow.class);
        when(row.getReleaseAt()).thenReturn(releaseAt);
        when(row.getCloseReason()).thenReturn(closeReason);
        return row;
    }

    @Test
    @DisplayName("상담 시작: 정지·진행 중 상담이 없으면 최신 정책으로 AI_CHAT 상담을 만든다")
    void startWhenCanStart() {
        // 준비
        givenCanStart();

        // 실행
        ChatStartResponseDTO response = customerChatService.start(CUSTOMER_ID);

        // 확인
        assertThat(response.status()).isEqualTo(ConsultationStatus.AI_CHAT);
        assertThat(response.createdAt()).isNotNull();
        verify(consultationRepository).save(argThat(saved ->
            saved.getCustomerId() == CUSTOMER_ID && Long.valueOf(7L).equals(saved.getPolicyId())));
    }

    @Test
    @DisplayName("상담 시작: 정지 중이면 CUSTOMER_SANCTIONED, data 에 사유와 해제 일시")
    void startWhenSanctioned() {
        givenCanStart();
        LocalDateTime releaseAt = LocalDateTime.of(2026, 11, 24, 14, 31);
        ActiveSanctionRow row = sanctionRow(releaseAt, CloseReason.ABUSE_AUTO);
        when(customerConsultationRepository.findActiveSanction(eq(CUSTOMER_ID), any())).thenReturn(Optional.of(row));

        assertThatThrownBy(() -> customerChatService.start(CUSTOMER_ID))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> {
                BusinessException be = (BusinessException) e;
                assertThat(be.getErrorCode()).isEqualTo(ErrorCode.CUSTOMER_SANCTIONED);
                assertThat(be.getMessage()).endsWith("2026-11-24 14:31에 해제됩니다.");
                assertThat(be.getData()).isEqualTo(new SanctionNoticeDTO("상담 중 반복 폭언 (자동 종료)", releaseAt));
            });
        verify(consultationRepository, never()).save(any());
    }

    @Test
    @DisplayName("상담 시작: 상담원 강제 종료로 정지됐으면 사유가 '상담원 종료'")
    void startWhenSanctionedByAgent() {
        givenCanStart();
        ActiveSanctionRow row = sanctionRow(LocalDateTime.of(2026, 11, 24, 14, 31), CloseReason.ABUSE_MANUAL);
        when(customerConsultationRepository.findActiveSanction(eq(CUSTOMER_ID), any())).thenReturn(Optional.of(row));

        assertThatThrownBy(() -> customerChatService.start(CUSTOMER_ID))
            .extracting(e -> ((SanctionNoticeDTO) ((BusinessException) e).getData()).reason())
            .isEqualTo("상담 중 폭언 (상담원 종료)");
    }

    @Test
    @DisplayName("상담 시작: 정지 중이면서 진행 중 상담도 있으면 정지를 먼저 알린다")
    void startWhenSanctionedAndOpenChat() {
        givenCanStart();
        ActiveSanctionRow row = sanctionRow(LocalDateTime.of(2026, 11, 24, 14, 31), CloseReason.ABUSE_AUTO);
        when(customerConsultationRepository.findActiveSanction(eq(CUSTOMER_ID), any())).thenReturn(Optional.of(row));
        when(customerConsultationRepository.findFirstByCustomerIdAndStatusIn(eq(CUSTOMER_ID), any()))
            .thenReturn(Optional.of(Consultation.start(CUSTOMER_ID, 1L)));

        assertThatThrownBy(() -> customerChatService.start(CUSTOMER_ID))
            .extracting("errorCode")
            .isEqualTo(ErrorCode.CUSTOMER_SANCTIONED);
    }

    @Test
    @DisplayName("상담 시작: 진행 중 상담이 있으면 CHAT_ALREADY_OPEN, data 에 기존 상담 상태")
    void startWhenOpenChatExists() {
        givenCanStart();
        Consultation open = Consultation.start(CUSTOMER_ID, 1L);
        open.handoff();   // QUEUED
        when(customerConsultationRepository.findFirstByCustomerIdAndStatusIn(eq(CUSTOMER_ID), any()))
            .thenReturn(Optional.of(open));

        assertThatThrownBy(() -> customerChatService.start(CUSTOMER_ID))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> {
                BusinessException be = (BusinessException) e;
                assertThat(be.getErrorCode()).isEqualTo(ErrorCode.CHAT_ALREADY_OPEN);
                assertThat(be.getData()).isEqualTo(new OpenChatDTO(open.getConsultationId(), ConsultationStatus.QUEUED));
            });
        verify(consultationRepository, never()).save(any());
    }

    @Test
    @DisplayName("상담 시작: 회원이 없으면 MEMBER_NOT_FOUND")
    void startWhenMemberNotFound() {
        when(customerConsultationRepository.lockCustomer(CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerChatService.start(CUSTOMER_ID))
            .extracting("errorCode")
            .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    // ===== CUS-03 상담원 연결 요청 =====

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

    // ===== CUS-04 cancel =====

    // cancel()은 락 없이 findById 로 조회한다
    private void givenConsultationForCancel(Consultation consultation) {
        when(consultationRepository.findById(CHAT_ID)).thenReturn(Optional.of(consultation));
    }

    // 조건부 UPDATE 가 바꾼 행 수 (1: 성공, 0: 그 사이 상태가 바뀜)
    private void givenCloseResult(int updatedRows) {
        when(customerConsultationRepository.closeByCustomer(eq(CHAT_ID), any(), any()))
            .thenReturn(updatedRows);
    }

    @Test
    @DisplayName("AI 상담 중이면 NORMAL 사유로 종료하고 종료 이벤트를 발행한다")
    void cancelWhenAiChat() {
        // 준비
        givenConsultationForCancel(Consultation.start(CUSTOMER_ID, 1L));
        givenCloseResult(1);

        // 실행
        CancelResponseDTO response = customerChatService.cancel(CHAT_ID, CUSTOMER_ID);

        // 확인
        assertThat(response.status()).isEqualTo(ConsultationStatus.CLOSED);
        assertThat(response.closeReason()).isEqualTo(CloseReason.NORMAL);
        assertThat(response.closedAt()).isNotNull();
        verify(eventPublisher).publishEvent(new ConsultationClosedEvent(CHAT_ID));
    }

    @Test
    @DisplayName("대기 중이면 QUEUE_CANCEL 사유로 종료하고 종료 이벤트를 발행한다")
    void cancelWhenQueued() {
        // 준비
        Consultation consultation = Consultation.start(CUSTOMER_ID, 1L);
        consultation.handoff();
        givenConsultationForCancel(consultation);
        givenCloseResult(1);

        // 실행
        CancelResponseDTO response = customerChatService.cancel(CHAT_ID, CUSTOMER_ID);

        // 확인
        assertThat(response.status()).isEqualTo(ConsultationStatus.CLOSED);
        assertThat(response.closeReason()).isEqualTo(CloseReason.QUEUE_CANCEL);
        verify(eventPublisher).publishEvent(new ConsultationClosedEvent(CHAT_ID));
    }

    @Test
    @DisplayName("이미 종료된 상담이면 예외 없이 현재 상태를 응답하고 이벤트는 발행하지 않는다")
    void cancelWhenAlreadyClosed() {
        // 준비
        Consultation consultation = Consultation.start(CUSTOMER_ID, 1L);
        consultation.close(ClosedBy.CUSTOMER, CloseReason.NORMAL);
        givenConsultationForCancel(consultation);

        // 실행
        CancelResponseDTO response = customerChatService.cancel(CHAT_ID, CUSTOMER_ID);

        // 확인
        assertThat(response.status()).isEqualTo(ConsultationStatus.CLOSED);
        assertThat(response.closeReason()).isEqualTo(CloseReason.NORMAL);
        verify(customerConsultationRepository, never()).closeByCustomer(anyLong(), any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("상담원과 연결된 상담이면 CHAT_ALREADY_ACTIVE")
    void cancelWhenInProgress() {
        // 준비: 실제 흐름대로 AI_CHAT → QUEUED → IN_PROGRESS
        Consultation consultation = Consultation.start(CUSTOMER_ID, 1L);
        consultation.handoff();
        consultation.accept(10L);
        givenConsultationForCancel(consultation);

        // 실행 + 확인
        assertThatThrownBy(() -> customerChatService.cancel(CHAT_ID, CUSTOMER_ID))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.CHAT_ALREADY_ACTIVE);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("조회 후 종료 전에 상담원이 수락하면 CHAT_ALREADY_ACTIVE (체크리스트 E5)")
    void cancelWhenAcceptedMeanwhile() {
        // 준비: 조회 시점엔 QUEUED 였지만 UPDATE 시점엔 이미 수락돼 0행이 바뀜
        Consultation consultation = Consultation.start(CUSTOMER_ID, 1L);
        consultation.handoff();
        givenConsultationForCancel(consultation);
        givenCloseResult(0);

        // 실행 + 확인
        assertThatThrownBy(() -> customerChatService.cancel(CHAT_ID, CUSTOMER_ID))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.CHAT_ALREADY_ACTIVE);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("다른 고객의 상담이면 CHAT_ACCESS_DENIED")
    void cancelWhenNotOwner() {
        // 준비: 주인은 999번 고객
        givenConsultationForCancel(Consultation.start(999L, 1L));

        // 실행 + 확인: 100번이 요청
        assertThatThrownBy(() -> customerChatService.cancel(CHAT_ID, CUSTOMER_ID))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.CHAT_ACCESS_DENIED);
        verify(eventPublisher, never()).publishEvent(any());
    }
}
