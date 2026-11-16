package com.safedesk.history.summary;

import com.safedesk.chat.entity.Consultation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;


import java.util.List;
import java.util.Optional;

// 상담 요약에 필요한 조회·저장만 모은 Repository
public interface ConsultationSummaryRepository extends Repository<Consultation, Long> {

    Optional<Consultation> findById(Long consultationId);

    // 고객 메시지를 오래된 순으로 조회 (첫 메시지만 쓰려면 PageRequest.of(0, 1))
    @Query("""
        select m.content from Message m
        where m.consultationId = :consultationId and m.senderType = 'CUSTOMER'
        order by m.createdAt asc, m.messageId asc
        """)
    List<String> findCustomerMessages(
        @Param("consultationId") Long consultationId,
        Pageable pageable
    );

    // 폭언으로 판정된 메시지 수
    @Query("""
            select count(m) from Message m
            where m.consultationId = :consultationId
            and m.abuseStatus = 'ABUSIVE'
           """)
    long countAbusiveMessages(@Param("consultationId") Long consultationId);

    // 요약이 아직 없을 때만 저장한다. 같은 이벤트가 두 번 와도 한 번만 저장된다.
    @Modifying
    @Query("""
            update Consultation c
            set c.inquiryCategory = :inquiryCategory,
                c.inquirySummary = :inquirySummary,
                c.processSummary = :processSummary,
                c.result = :result
            where c.consultationId = :consultationId and c.processSummary is null
        """)
    int updateSummaryIfEmpty(@Param("consultationId") Long consultationId,
                           @Param("inquiryCategory") String inquiryCategory,
                           @Param("inquirySummary") String inquirySummary,
                           @Param("processSummary") String processSummary,
                           @Param("result") String result);

    // 요약 실패 시 "요약 없음" 저장. 문의 유형·처리 결과는 건드리지 않는다.
    @Modifying
    @Query("""
            update Consultation c
            set c.inquirySummary = :emptyText,
                c.processSummary = :emptyText
            where c. consultationId = :consultationId and c.processSummary is null
           """)
    int markSummaryFailed(@Param("consultationId") Long consultationId,
                          @Param("emptyText") String emptyText);
}
