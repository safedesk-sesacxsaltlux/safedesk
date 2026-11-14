package com.safedesk.moderation.filter;

/**
 * 정규화된 문자열과, 정규화 문자열의 각 글자가 원문 몇 번째 글자에서 왔는지를 담는다.
 *
 * 예) 원문 "시 발!" → text "시발", originalIndex [0, 2]
 *     정규화 문자열에서 [0, 2) 구간이 매칭되면 원문 [originalIndex[0], originalIndex[1] + 1) = [0, 3) 를 가린다.
 *
 * @param text          공백·특수문자를 제거한 문자열
 * @param originalIndex originalIndex[i] = text.charAt(i) 의 원문 위치
 */
public record NormalizedText(String text, int[] originalIndex) {
}
