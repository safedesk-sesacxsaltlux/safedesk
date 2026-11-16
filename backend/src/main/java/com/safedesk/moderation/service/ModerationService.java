package com.safedesk.moderation.service;

import com.safedesk.moderation.dto.AbuseFilterResult;
import com.safedesk.moderation.entity.AbuseLog;
import com.safedesk.moderation.filter.AbuseFilter;
import com.safedesk.moderation.repository.AbuseLogRepository;
import org.springframework.stereotype.Service;

/**
 * 폭언 판정·원문 저장 (MOD-01, docs/api/moderation.md).
 *
 * 메서드를 둘로 나눈 이유: abuse_log 에는 message_id 가 필요한데, 이 값은 메시지를 저장한 뒤에야 생긴다.
 * 그래서 ChatService 가 「① 판정 → 메시지 저장 → ② 원문 저장」 순서로 호출한다.
 *
 * @ Transactional 은 붙이지 않는다. ChatService.sendMessage() 의 트랜잭션 안에서 같이 실행된다.
 */
@Service
public class ModerationService {

    private final AbuseFilter abuseFilter;
    private final AbuseLogRepository abuseLogRepository;

    public ModerationService(AbuseFilter abuseFilter, AbuseLogRepository abuseLogRepository) {
        this.abuseFilter = abuseFilter;
        this.abuseLogRepository = abuseLogRepository;
    }

    /**
     * ① 판정만 한다. ChatService 가 메시지를 저장하기 「전」에 호출한다.
     *
     * @return NORMAL 이면 원문 그대로, ABUSIVE 이면 금칙어만 *** 로 가린 문장
     */
    public AbuseFilterResult processMessage(String content) {
        return abuseFilter.filter(content);
    }

    /**
     * ② 원문 저장. ChatService 가 메시지를 저장한 「뒤」, ABUSIVE 일 때만 호출한다.
     *
     * @return 저장된 AbuseLog (MOD-02 에서 경고에 abuse_log_id 를 연결할 때 쓴다)
     */
    public AbuseLog saveOriginal(long messageId, long consultationId, String originalContent) {
        return abuseLogRepository.save(new AbuseLog(messageId, consultationId, originalContent));
    }
}
