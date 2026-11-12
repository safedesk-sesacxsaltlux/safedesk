package com.safedesk.moderation.filter;

import com.safedesk.moderation.dto.AbuseFilterResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 금칙어 규칙 기반 폭언 필터 (REQ-014, REQ-015).
 *
 * 상담 상태(IN_PROGRESS)·발신자(고객) 검사는 호출부(ChatService)가 한다. 여기서는 문장만 본다.
 * 메시지·원문 저장(message, abuse_log)은 D3 이슈 범위.
 */
@Component
public class AbuseFilter {

    private static final String MASK = "***";

    /**
     * 오탐 방지용 예외 표현 (정규화된 형태로 적는다).
     * 공백을 지우면 「다시 발송」 → 「다시발송」 이 되어 「시발」이 잡히는 문제를 막는다.
     * 금칙어가 이 표현 안에 완전히 들어가 있으면 무시한다.
     */
    private static final List<String> ALLOWED_PHRASES = List.of(
            "다시발",      // 다시 발송, 다시 발급
            "시발점",      // 시발점(始發點)
            "시발역",
            "출시발표",    // 출시 발표
            "질병신고",    // 질병 신고
            "이미친구",    // 이미 친구 (「이미친」으로 줄이면 「이 미친 ~」까지 통과하므로 금지)
            "새끼손가락"
            // 새 예외를 넣을 땐 너무 짧게 잡아 진짜 욕설까지 통과시키지 않는지 AbuseFilterTest 에서 확인
    );

    private final BannedWordCache bannedWordCache;
    private final TextNormalizer textNormalizer;

    public AbuseFilter(BannedWordCache bannedWordCache, TextNormalizer textNormalizer) {
        this.bannedWordCache = bannedWordCache;
        this.textNormalizer = textNormalizer;
    }

    public AbuseFilterResult filter(String content) {
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        if (content.isBlank()) {
            return AbuseFilterResult.normal(content);   // 빈 메시지 거부는 호출부 책임 (INVALID_MESSAGE)
        }

        NormalizedText normalized = textNormalizer.normalize(content);
        List<int[]> ranges = findBannedRanges(normalized.text());

        if (ranges.isEmpty()) {
            return AbuseFilterResult.normal(content);
        }
        return AbuseFilterResult.abusive(mask(content, normalized, mergeRanges(ranges)));
    }

    /**
     * 정규화 문자열에서 금칙어가 나타나는 모든 구간 [start, end) 를 찾는다.
     */
    private List<int[]> findBannedRanges(String text) {
        List<int[]> ranges = new ArrayList<>();
        for (String word : bannedWordCache.getWords()) {
            int start = text.indexOf(word);
            while (start != -1) {
                int end = start + word.length();
                if (!isInsideAllowedPhrase(text, start, end)) {
                    ranges.add(new int[]{start, end});
                }
                start = text.indexOf(word, start + 1);
            }
        }
        return ranges;
    }

    /**
     * [start, end) 가 예외 표현 안에 완전히 들어가 있는가?
     */
    private boolean isInsideAllowedPhrase(String text, int start, int end) {
        for (String phrase : ALLOWED_PHRASES) {
            int p = text.indexOf(phrase);
            while (p != -1) {
                if (p <= start && end <= p + phrase.length()) {
                    return true;
                }
                p = text.indexOf(phrase, p + 1);
            }
        }
        return false;
    }

    /**
     * 겹치거나 맞닿은 구간을 하나로 합친다.
     * 예) 「개새끼」 [0,3) 와 「새끼」 [1,3) → [0,3) 하나
     *     [0,2) 와 [2,4) → [0,4) 하나 (*** 가 두 번 붙지 않게)
     */
    private List<int[]> mergeRanges(List<int[]> ranges) {
        ranges.sort(Comparator.comparingInt(r -> r[0]));   // 시작 위치 순으로 정렬

        List<int[]> merged = new ArrayList<>();
        for (int[] range : ranges) {
            // 첫 구간이거나, 마지막 구간과 떨어져 있으면 → 새 구간으로 추가
            if (merged.isEmpty() || merged.get(merged.size() - 1)[1] < range[0]) {
                merged.add(range);
                continue;
            }
            // 겹치거나 맞닿음 → 마지막 구간의 end 를 둘 중 큰 값으로 늘림
            int[] last = merged.get(merged.size() - 1);
            last[1] = Math.max(last[1], range[1]);
        }
        return merged;
    }

    /**
     * 정규화 기준 구간을 원문 기준 구간으로 바꿔 *** 로 치환한다.
     */
    private String mask(String content, NormalizedText normalized, List<int[]> merged) {
        int[] originalIndex = normalized.originalIndex();
        StringBuilder masked = new StringBuilder(content);

        // 뒤 구간부터 치환해야 앞 구간의 원문 인덱스가 밀리지 않는다
        for (int i = merged.size() - 1; i >= 0; i--) {
            int[] range = merged.get(i);

            // 정규화 구간 [range[0], range[1]) → 원문 구간 [originalStart, originalEnd)
            //   예) 원문 "ㅆ!발" → 정규화 "ㅆ발", originalIndex [0, 2], range [0, 2)
            //       originalStart = 0, originalEnd = 3  (마지막 글자 「발」의 원문 위치 2 + 1)
            //   range[1] 은 「끝 다음」 위치라서 마지막 글자(range[1] - 1)의 원문 위치에 1 을 더한다.
            int originalStart = originalIndex[range[0]];
            int originalEnd = originalIndex[range[1] - 1] + 1;

            masked.replace(originalStart, originalEnd, MASK);
        }
        return masked.toString();
    }
}
