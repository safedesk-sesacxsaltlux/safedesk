package com.safedesk.auth.jwt;

import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// JWT 발급과 검증을 한 곳에서 같은 키로 처리한다.
// 검증은 HTTP 요청(/api/auth/me 등)과 WebSocket STOMP CONNECT 인증에서 함께 쓴다.
@Component
public class JwtProvider {

    private static final String ROLE_CLAIM = "role";

    private final SecretKey key;
    private final long expirationSeconds;

    public JwtProvider(@Value("${jwt.secret}") String secret,
                       @Value("${jwt.expiration-seconds}") long expirationSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expirationSeconds;
    }

    // payload에는 sub(memberId), role, exp만 넣는다. (auth.md API-AUTH-03)
    public String createToken(Long memberId, String role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationSeconds * 1000);

        return Jwts.builder()
            .subject(String.valueOf(memberId))
            .claim(ROLE_CLAIM, role)
            .issuedAt(now)
            .expiration(expiration)
            .signWith(key)
            .compact();
    }

    // 서명 위조·변조, 만료, 형식 오류를 모두 확인하고 통과하면 memberId와 role을 꺼낸다.
    public JwtClaims validate(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
            return new JwtClaims(Long.valueOf(claims.getSubject()), claims.get(ROLE_CLAIM, String.class));
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}
