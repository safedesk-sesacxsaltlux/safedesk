package com.safedesk.history.summary;

import com.safedesk.common.event.ConsultationClosedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
// 이벤트 받기 + 실패 처리
public class ConsultationSummaryListener {

    private final ConsultationSummaryService summaryService;

    // AFTER_COMMIT: 종료가 DB에 확정된 뒤에만 실행. 종료가 롤백되면 요약도 하지 않는다.
    // fallbackExecution: 트랜잭션 밖에서 발행돼도 요약이 빠지지 않게 바로 실행한다.

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onConsultationClosed(ConsultationClosedEvent event) {
        Long consultationId = event.consultationId();
        try{
            summaryService.summarize(consultationId);
        } catch (Exception e) {
            // 여기서 예외를 밖으로 던지면 이미 끝난 종료 API가 500으로 응답한다.
            log.error("상담 요약 실패: consultationId={}" , consultationId, e);
            saveFailure(consultationId);
        }
    }
    private void saveFailure(Long consultationId) {
        try {
            summaryService.markFailed(consultationId);
        } catch (Exception e) {
            log.error("요약 실패 기록 저장도 실패: consultationId={}", consultationId, e);
        }
    }
}
