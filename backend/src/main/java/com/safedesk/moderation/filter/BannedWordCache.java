package com.safedesk.moderation.filter;

import com.safedesk.moderation.entity.BannedWord;
import com.safedesk.moderation.repository.BannedWordRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * banned_word 를 메모리에 캐싱한다. 메시지마다 DB 를 조회하지 않기 위함 (NFR-007 전달 지연 1초 이내).
 */
@Component
public class BannedWordCache {

    private final BannedWordRepository bannedWordRepository;
    private final TextNormalizer textNormalizer;

    // 정규화된 금칙어 목록. 재적재 시 통째로 교체하므로 volatile.
    private volatile List<String> words = List.of();

    public BannedWordCache(BannedWordRepository bannedWordRepository, TextNormalizer textNormalizer) {
        this.bannedWordRepository = bannedWordRepository;
        this.textNormalizer = textNormalizer;
    }

    /**
     * 앱 시작 시 한 번 적재한다. DB 조회에 실패하면 예외가 그대로 올라가 앱이 뜨지 않는다.
     * (금칙어 없이 상담이 열리면 원문이 상담원에게 그대로 노출되므로, 뜨지 않는 편이 안전하다)
     */
    @EventListener(ApplicationReadyEvent.class)
    public void load() {
        words = bannedWordRepository.findAll().stream()
                .map(BannedWord::getWord)
                .map(textNormalizer::normalizeWord)   // 「시 발」 → 「시발」
                .filter(word -> !word.isEmpty())
                .distinct()                           // 「시발」과 「시 발」이 하나로 합쳐짐
                .toList();
    }

    public List<String> getWords() {
        return words;
    }
}
