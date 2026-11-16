package com.safedesk.auth.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

// 리프레시 토큰 쿠키 설정을 한 곳에서 관리한다. (auth.md 토큰 정책)
// HttpOnly: JS가 읽지 못함 / SameSite=Strict: 다른 사이트에서 보낸 요청에는 붙지 않음 (CSRF 방지)
// Path=/api/auth: 로그인·재발급·로그아웃 요청에만 붙고 다른 API 요청에는 붙지 않음
@Component
public class RefreshTokenCookie {

    public static final String NAME = "refreshToken";
    private static final String PATH = "/api/auth";

    private final long maxAgeSeconds;
    private final boolean secure;

    public RefreshTokenCookie(@Value("${jwt.refresh-expiration-seconds}") long maxAgeSeconds,
                              @Value("${jwt.refresh-cookie-secure}") boolean secure) {
        this.maxAgeSeconds = maxAgeSeconds;
        this.secure = secure;
    }

    public String create(String token) {
        return build(token, maxAgeSeconds);
    }

    // 같은 이름·경로로 Max-Age=0 쿠키를 보내면 브라우저가 지운다. HttpOnly 쿠키는 JS로 못 지우므로 서버가 지워야 한다.
    public String expire() {
        return build("", 0);
    }

    private String build(String value, long maxAge) {
        return ResponseCookie.from(NAME, value)
            .httpOnly(true)
            .secure(secure)
            .sameSite("Strict")
            .path(PATH)
            .maxAge(maxAge)
            .build()
            .toString();
    }
}
