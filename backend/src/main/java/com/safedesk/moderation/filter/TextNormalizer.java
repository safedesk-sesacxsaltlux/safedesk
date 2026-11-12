package com.safedesk.moderation.filter;

import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 변형 표현(「시 발」, 「ㅆ!발」 등) 대응을 위한 메시지 정규화.
 */
@Component
public class TextNormalizer {

    /**
     * 원문에서 공백·특수문자를 제거하고, 남은 글자마다 원문 위치를 기록한다.
     * 예) "ㅆ!발 X" → text "ㅆ발x", originalIndex [0, 2, 4]
     */
    public NormalizedText normalize(String original) {
        StringBuilder text = new StringBuilder();
        int[] originalIndex = new int[original.length()];

        for (int i = 0; i < original.length(); i++) {
            char c = original.charAt(i);
            if (!isKept(c)) {
                continue;
            }
            originalIndex[text.length()] = i;          // 정규화 문자열의 이 자리는 원문 i 번째 글자
            text.append(Character.toLowerCase(c));     // 「씨X」와 「씨x」를 같게 본다
        }

        // 남긴 글자 수만큼만 잘라서 넘긴다
        return new NormalizedText(text.toString(), Arrays.copyOf(originalIndex, text.length()));
    }

    /**
     * 금칙어 자체를 정규화한다. (「시 발」 → 「시발」 처럼 시드의 띄어쓰기 변형이 하나로 합쳐진다)
     */
    public String normalizeWord(String word) {
        return normalize(word).text();
    }

    /**
     * 정규화 후에도 남길 글자인가?
     * 남길 것: 한글 완성형(가~힣), 자음·모음(ㄱ~ㅎ, ㅏ~ㅣ), 영문, 숫자
     * 버릴 것: 공백, 특수문자(! . , ? * 등)
     */
    private boolean isKept(char c) {
        // 한글 완성형·호환용 자모(ㄱ~ㅎ, ㅏ~ㅣ)도 유니코드 letter 로 분류되어 true 가 나온다
        return Character.isLetterOrDigit(c);
    }
}
