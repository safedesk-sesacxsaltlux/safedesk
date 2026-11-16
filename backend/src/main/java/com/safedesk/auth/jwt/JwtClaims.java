package com.safedesk.auth.jwt;

import java.time.Instant;

// expiresAt은 로그아웃 시 블랙리스트 보관 기간으로 쓴다.
public record JwtClaims(Long memberId, String role, Instant expiresAt) {
}
