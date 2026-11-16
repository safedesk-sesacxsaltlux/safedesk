
package com.safedesk.chat.entity;

import com.safedesk.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.*;

public class ConsultationTest {

    private Consultation createConsultation(String status) throws Exception {
        Consultation consultation = new Consultation();

        Field field = Consultation.class.getDeclaredField("status");
        field.setAccessible(true);
        field.set(consultation, status);

        return consultation;
    }

    @Test
    @DisplayName("AI 상담을 대기열로 전환한다")
    void handoffToQueue() throws Exception {
        Consultation consultation = createConsultation(ConsultationStatus.AI_CHAT);

        consultation.handoff();

        assertThat(consultation.getStatus()).isEqualTo(ConsultationStatus.QUEUED);
        assertThat(consultation.getQueuedAt()).isNotNull();
    }

    @Test
    @DisplayName("대기 중인 상담을 수락한다")
    void acceptQueued() throws Exception {
        Consultation consultation = createConsultation(ConsultationStatus.QUEUED);

        consultation.accept(10L);

        assertThat(consultation.getStatus()).isEqualTo(ConsultationStatus.IN_PROGRESS);
        assertThat(consultation.getAgentId()).isEqualTo(10L);
        assertThat(consultation.getAcceptedAt()).isNotNull();
    }

    @Test
    @DisplayName("진행 중인 상담을 정상 종료한다")
    void closeInProgress() throws Exception {
        Consultation consultation = createConsultation(ConsultationStatus.IN_PROGRESS);

        boolean result = consultation.close(ClosedBy.AGENT, CloseReason.NORMAL);

        assertThat(result).isTrue();
        assertThat(consultation.getStatus()).isEqualTo(ConsultationStatus.CLOSED);
        assertThat(consultation.getClosedAt()).isNotNull();
    }

    @Test
    @DisplayName("진행 중인 상담을 강제 종료한다")
    void terminateInProgress() throws Exception {
        Consultation consultation = createConsultation(ConsultationStatus.IN_PROGRESS);

        boolean result = consultation.terminate(ClosedBy.SYSTEM, CloseReason.ABUSE_AUTO);

        assertThat(result).isTrue();
        assertThat(consultation.getStatus()).isEqualTo(ConsultationStatus.TERMINATED);
        assertThat(consultation.getClosedAt()).isNotNull();
    }

    @Test
    @DisplayName("진행 중인 상담은 다시 수락할 수 없다")
    void rejectAcceptInProgress() throws Exception {
        Consultation consultation = createConsultation(ConsultationStatus.IN_PROGRESS);

        assertThatThrownBy(() -> consultation.accept(10L))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("이미 종료된 상담은 다시 종료하지 않는다")
    void closeAlreadyClosed() throws Exception {
        Consultation consultation = createConsultation(ConsultationStatus.CLOSED);

        boolean result = consultation.close(ClosedBy.AGENT, CloseReason.NORMAL);

        assertThat(result).isFalse();
        assertThat(consultation.getStatus()).isEqualTo(ConsultationStatus.CLOSED);
    }

    @Test
    @DisplayName("이미 강제 종료된 상담은 중복 종료하지 않는다")
    void terminateAlreadyTerminated() throws Exception {
        Consultation consultation = createConsultation(ConsultationStatus.TERMINATED);

        boolean result = consultation.terminate(ClosedBy.SYSTEM, CloseReason.ABUSE_AUTO);

        assertThat(result).isFalse();
        assertThat(consultation.getStatus()).isEqualTo(ConsultationStatus.TERMINATED);
    }

    @Test
    @DisplayName("대기 중인 상담은 강제 종료할 수 없다")
    void rejectTerminateQueued() throws Exception {
        Consultation consultation = createConsultation(ConsultationStatus.QUEUED);

        assertThatThrownBy(() -> consultation.terminate(ClosedBy.SYSTEM, CloseReason.ABUSE_AUTO))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void 처음_매칭된_FAQ와_문의_유형을_저장한다() throws Exception {
        Consultation consultation = createConsultation(ConsultationStatus.AI_CHAT);

        consultation.recordFirstFaqMatch(1L, "상하수도");

        assertThat(consultation.getFaqId()).isEqualTo(1L);
        assertThat(consultation.getInquiryCategory()).isEqualTo("상하수도");
    }

    @Test
    void 두_번째_매칭부터는_덮어쓰지_않는다() throws Exception {
        Consultation consultation = createConsultation(ConsultationStatus.AI_CHAT);
        consultation.recordFirstFaqMatch(1L, "상하수도");

        consultation.recordFirstFaqMatch(22L, "대중교통");

        assertThat(consultation.getFaqId()).isEqualTo(1L);
        assertThat(consultation.getInquiryCategory()).isEqualTo("상하수도");
    }
}
