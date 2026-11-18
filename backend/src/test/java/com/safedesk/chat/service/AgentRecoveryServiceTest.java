package com.safedesk.chat.service;

import com.safedesk.chat.dto.AgentRecoveryResponseDTO;
import com.safedesk.chat.entity.Consultation;
import com.safedesk.chat.entity.ConsultationStatus;
import com.safedesk.chat.repository.ConsultationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentRecoveryServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 18, 14, 0);

    private final ConsultationRepository repository = mock(ConsultationRepository.class);
    private final AgentRecoveryService service =
        new AgentRecoveryService(repository, Clock.fixed(NOW.atZone(KST).toInstant(), KST));

    @Test
    @DisplayName("폭언 종료 후 10분 안이면 회복 중이고 남은 시간을 준다 (REQ-033)")
    void recovering() {
        givenLastTerminatedAt(NOW.minusMinutes(3));

        AgentRecoveryResponseDTO result = service.getRecovery(10L);

        assertThat(result.isRecovering()).isTrue();
        assertThat(result.canAcceptChat()).isFalse();
        assertThat(result.recoveryEndsAt()).isEqualTo(NOW.plusMinutes(7));
        assertThat(result.remainingSeconds()).isEqualTo(7 * 60);
    }

    @Test
    @DisplayName("10분이 지났으면 회복 중이 아니다")
    void recovered() {
        givenLastTerminatedAt(NOW.minusMinutes(10));

        AgentRecoveryResponseDTO result = service.getRecovery(10L);

        assertThat(result.isRecovering()).isFalse();
        assertThat(result.canAcceptChat()).isTrue();
        assertThat(result.remainingSeconds()).isZero();
    }

    @Test
    @DisplayName("폭언 종료된 상담이 없으면 회복 중이 아니다")
    void neverTerminated() {
        when(repository.findFirstByAgentIdAndStatusOrderByClosedAtDesc(10L, ConsultationStatus.TERMINATED))
            .thenReturn(Optional.empty());

        assertThat(service.getRecovery(10L).isRecovering()).isFalse();
    }

    @Test
    @DisplayName("진행 중 상담이 있으면 그 번호를 주고 수락할 수 없다고 알려준다")
    void busyAgent() {
        when(repository.findFirstByAgentIdAndStatusOrderByClosedAtDesc(10L, ConsultationStatus.TERMINATED))
            .thenReturn(Optional.empty());
        Consultation active = mock(Consultation.class);
        when(active.getConsultationId()).thenReturn(7L);
        when(repository.findFirstByAgentIdAndStatusOrderByAcceptedAtDesc(10L, ConsultationStatus.IN_PROGRESS))
            .thenReturn(Optional.of(active));

        AgentRecoveryResponseDTO result = service.getRecovery(10L);

        assertThat(result.isRecovering()).isFalse();
        assertThat(result.canAcceptChat()).isFalse();
        assertThat(result.activeConsultationId()).isEqualTo(7L);
    }

    private void givenLastTerminatedAt(LocalDateTime closedAt) {
        Consultation terminated = mock(Consultation.class);
        when(terminated.getClosedAt()).thenReturn(closedAt);
        when(repository.findFirstByAgentIdAndStatusOrderByClosedAtDesc(10L, ConsultationStatus.TERMINATED))
            .thenReturn(Optional.of(terminated));
    }
}
