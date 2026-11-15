package com.safedesk.auth.jwt;

// 검증을 통과한 토큰에서 꺼낸 값
public record JwtClaims(Long memberId, String role) {
}
