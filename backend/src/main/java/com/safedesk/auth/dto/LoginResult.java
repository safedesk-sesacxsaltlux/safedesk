package com.safedesk.auth.dto;

// refreshToken은 응답 본문이 아니라 쿠키로 보낸다.
public record LoginResult(LoginResponseDTO response, String refreshToken) {
}
