
package com.safedesk.chat.entity;

import com.safedesk.common.exception.BusinessException;
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
    void transitionsAiConsultationToQueue() throws Exception {
        Consultation consultation = createConsultation(ConsultationStatus.AI_CHAT);

        consultation.handoff();

        assertThat(consultation.getStatus()).isEqualTo(ConsultationStatus.QUEUED);
        assertThat(consultation.getQueuedAt()).isNotNull();
    }

    @Test
    void acceptsWaitingConsultation() throws Exception {
        Consultation consultation = createConsultation(ConsultationStatus.QUEUED);

        consultation.accept(10L);

        assertThat(consultation.getStatus()).isEqualTo(ConsultationStatus.IN_PROGRESS);
        assertThat(consultation.getAgentId()).isEqualTo(10L);
        assertThat(consultation.getAcceptedAt()).isNotNull();
    }

    @Test
    void closesInProgressConsultationNormally() throws Exception {
        Consultation consultation = createConsultation(ConsultationStatus.IN_PROGRESS);

        boolean result = consultation.close(ClosedBy.AGENT, CloseReason.NORMAL);

        assertThat(result).isTrue();
        assertThat(consultation.getStatus()).isEqualTo(ConsultationStatus.CLOSED);
        assertThat(consultation.getClosedAt()).isNotNull();
    }

    @Test
    void forceClosesInProgressConsultation() throws Exception {
        Consultation consultation = createConsultation(ConsultationStatus.IN_PROGRESS);

        boolean result = consultation.terminate(ClosedBy.SYSTEM, CloseReason.ABUSE_AUTO);

        assertThat(result).isTrue();
        assertThat(consultation.getStatus()).isEqualTo(ConsultationStatus.TERMINATED);
        assertThat(consultation.getClosedAt()).isNotNull();
    }

    @Test
    void cannotAcceptWhenNotWaiting() throws Exception {
        Consultation consultation = createConsultation(ConsultationStatus.IN_PROGRESS);

        assertThatThrownBy(() -> consultation.accept(10L))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void doesNotCloseAlreadyClosedConsultation() throws Exception {
        Consultation consultation = createConsultation(ConsultationStatus.CLOSED);

        boolean result = consultation.close(ClosedBy.AGENT, CloseReason.NORMAL);

        assertThat(result).isFalse();
        assertThat(consultation.getStatus()).isEqualTo(ConsultationStatus.CLOSED);
    }

    @Test
    void doesNotCloseAlreadyForceClosedConsultationAgain() throws Exception {
        Consultation consultation = createConsultation(ConsultationStatus.TERMINATED);

        boolean result = consultation.terminate(ClosedBy.SYSTEM, CloseReason.ABUSE_AUTO);

        assertThat(result).isFalse();
        assertThat(consultation.getStatus()).isEqualTo(ConsultationStatus.TERMINATED);
    }

    @Test
    void cannotForceCloseWhenNotInProgress() throws Exception {
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
