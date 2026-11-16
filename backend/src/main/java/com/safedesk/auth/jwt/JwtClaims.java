package com.safedesk.auth.jwt;

import java.time.Instant;

// 검증을 통과한 토큰에서 꺼낸 값. expiresAt은 로그아웃 시 블랙리스트 보관 기간으로 쓴다.
public record JwtClaims(Long memberId, String role, Instant expiresAt) {
}
