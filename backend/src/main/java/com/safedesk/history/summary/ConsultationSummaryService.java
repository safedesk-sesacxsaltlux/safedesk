package com.safedesk.history.summary;

import com.safedesk.chat.entity.Consultation;
import com.safedesk.faq.entity.Faq;
import com.safedesk.faq.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
// ⑤ ConsultationSummaryService: 순서 조립 + 트랜잭션
public class ConsultationSummaryService {

    private static final Set<String> CLOSED_STATUSES = Set.of("CLOSED", "TERMINATED");

    private final ConsultationSummaryRepository summaryRepository;
    private final ConsultationSummaryGenerator summaryGenerator;
    private final FaqRepository faqRepository;

// 상담 종료 트랜잭션은 이미 커밋된 뒤라 새 트랜잭션(REQUIRES_NEW)에서
// 저장해야 실제로 반영된다.

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void summarize(Long consultationId) {
        Consultation consultation = summaryRepository.findById(consultationId).orElse(null);
        // 상담이 존재하지 않으면 로그만 남기고 메서드 종료
        if(consultation == null) {
            log.warn("요약 대상 상담이 없음: consultationId={}", consultationId);
            return;
        }
        // 상담 상태가 CLOSED 또는 TERMINATED인지 확인
        if(!CLOSED_STATUSES.contains(consultation.getStatus())) {
            log.warn("종료되지 않은 상담은 요약하지 않음: consultationId={}, status={}",
                    consultationId, consultation.getStatus());
            return;
        }
        // 이미 요약된 상담인지 확인
        if (consultation.getProcessSummary() != null) {
            log.info("이미 요약된 상담: consultationId={}", consultationId);
            return;
        }


        // 고객 메시지를 오래된 순으로 첫 번째 1개만 조회
        List<String> firstMessage = summaryRepository.findCustomerMessages(
            consultationId,
            PageRequest.of(0,1)
        );
        String firstCustomerMessage = firstMessage.isEmpty() ? null : firstMessage.get(0);

        // 상담 중 처음 매칭된 FAQ (CUS-02 6단계에서 저장)의 대표 질문
        Long faqId = consultation.getFaqId();
        String faqQuestion = faqId == null ? null
                : faqRepository.findById(faqId).map(Faq::getQuestion).orElse(null);

        // 조회한 상담 정보를 SummarySource 객체로 묶음
        SummarySource source = new SummarySource(
            consultation.getStatus(),
            consultation.getClosedBy(),
            consultation.getCloseReason(),
            consultation.getQueuedAt(),
            consultation.getAcceptedAt(),
            consultation.getInquiryCategory(),
            consultation.getInquirySummary(),
            firstCustomerMessage,
            faqQuestion,
            faqId != null,
            summaryRepository.countAbusiveMessages(consultationId)
        );

        // SummarySource를 전달해서 요약 결과 생성
        ConsultationSummary summary = summaryGenerator.generate(source);

        // ⑦ 생성된 요약 결과 4개를 DB에 저장
        summaryRepository.updateSummaryIfEmpty(
            consultationId,
            summary.inquiryCategory(),
            summary.inquirySummary(),
            summary.processSummary(),
            summary.result()
        );
        // 요약 저장 작업에 대한 로그 기록
        log.info("상담 요약 저장: consultationId={}, result={}", consultationId, summary.result());
    }
    //요약 생성 실패 시 별도 트랜잭션으로 실패 문구 저장
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long consultationId) {
        summaryRepository.markSummaryFailed(consultationId, ConsultationSummary.EMPTY_TEXT);
    }

}
