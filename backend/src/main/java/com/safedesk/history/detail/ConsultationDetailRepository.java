package com.safedesk.history.detail;

import com.safedesk.chat.entity.Message;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConsultationDetailRepository extends Repository<Message, Long> {
    // 메서드 이름으로 쿼리 생성: where consultation_id = ? order by created_at, message_id
    List<Message> findByConsultationIdOrderByCreatedAtAscMessageIdAsc(long consultationId);
    // 폭언 원문 (고객 본인 조회용)
    @Query(value = """
                    select message_id as messageId, original_content as originalContent
                    from abuse_log
                    where consultation_id = :consultationId
                """, nativeQuery = true)
    List<AbuseOriginalRow>findAbuseOriginals(@Param("consultationId") long consultationId);

    // 경고를 발생 순서대로. 폭언 메시지와 연결 안 된 경고(수동 경고)도 순번에 들어가야 해서 left join
    @Query(value = """
                    select a.message_id
                    from warning w
                    left join abuse_log a on a.abuse_log_id = w.abuse_log_id
                    where w.consultation_id = :consultationId
                    order by w.created_at, w.warning_id
                """, nativeQuery = true)
    List<Long> findWarningMessageIdsInOrder(@Param("consultationId") long consultationId);

    // 위반 기록 수 (consultation_id UNIQUE 라 0 또는 1)
    @Query(value = "select count(*) from violation where consultation_id = :consultationId",
        nativeQuery = true)
    long countViolations(@Param("consultationId") long consultationId);
}
