
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
    void AI_상담을_대기열로_전환한다() throws Exception {
        Consultation consultation = createConsultation(ConsultationStatus.AI_CHAT);

        consultation.handoff();

        assertThat(consultation.getStatus()).isEqualTo(ConsultationStatus.QUEUED);
        assertThat(consultation.getQueuedAt()).isNotNull();
    }

    @Test
    void 대기_상담을_수락한다() throws Exception {
        Consultation consultation = createConsultation(ConsultationStatus.QUEUED);

        consultation.accept(10L);

        assertThat(consultation.getStatus()).isEqualTo(ConsultationStatus.IN_PROGRESS);
        assertThat(consultation.getAgentId()).isEqualTo(10L);
        assertThat(consultation.getAcceptedAt()).isNotNull();
    }

    @Test
    void 진행_중인_상담을_정상_종료한다() throws Exception {
        Consultation consultation = createConsultation(ConsultationStatus.IN_PROGRESS);

        boolean result = consultation.close(ClosedBy.AGENT, CloseReason.NORMAL);

        assertThat(result).isTrue();
        assertThat(consultation.getStatus()).isEqualTo(ConsultationStatus.CLOSED);
        assertThat(consultation.getClosedAt()).isNotNull();
    }

    @Test
    void 진행_중인_상담을_강제_종료한다() throws Exception {
        Consultation consultation = createConsultation(ConsultationStatus.IN_PROGRESS);

        boolean result = consultation.terminate(ClosedBy.SYSTEM, CloseReason.ABUSE_AUTO);

        assertThat(result).isTrue();
        assertThat(consultation.getStatus()).isEqualTo(ConsultationStatus.TERMINATED);
        assertThat(consultation.getClosedAt()).isNotNull();
    }

    @Test
    void 대기_상태가_아니면_수락할_수_없다() throws Exception {
        Consultation consultation = createConsultation(ConsultationStatus.IN_PROGRESS);

        assertThatThrownBy(() -> consultation.accept(10L))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void 이미_종료된_상담은_다시_종료하지_않는다() throws Exception {
        Consultation consultation = createConsultation(ConsultationStatus.CLOSED);

        boolean result = consultation.close(ClosedBy.AGENT, CloseReason.NORMAL);

        assertThat(result).isFalse();
        assertThat(consultation.getStatus()).isEqualTo(ConsultationStatus.CLOSED);
    }

    @Test
    void 이미_강제_종료된_상담은_중복_종료하지_않는다() throws Exception {
        Consultation consultation = createConsultation(ConsultationStatus.TERMINATED);

        boolean result = consultation.terminate(ClosedBy.SYSTEM, CloseReason.ABUSE_AUTO);

        assertThat(result).isFalse();
        assertThat(consultation.getStatus()).isEqualTo(ConsultationStatus.TERMINATED);
    }

    @Test
    void 진행_중이_아니면_강제_종료할_수_없다() throws Exception {
        Consultation consultation = createConsultation(ConsultationStatus.QUEUED);

        assertThatThrownBy(() -> consultation.terminate(ClosedBy.SYSTEM, CloseReason.ABUSE_AUTO))
            .isInstanceOf(BusinessException.class);
    }
}
