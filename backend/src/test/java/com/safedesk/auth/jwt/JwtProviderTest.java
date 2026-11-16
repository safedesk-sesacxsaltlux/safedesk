package com.safedesk.auth.jwt;

import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JwtProviderTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-bytes!!";

    @Test
    void 발급한_토큰을_검증하면_memberId와_role을_꺼낸다() {
        JwtProvider provider = new JwtProvider(SECRET, 3600);

        String token = provider.createToken(1L, "CUSTOMER");
        JwtClaims claims = provider.validate(token);

        assertThat(claims.memberId()).isEqualTo(1L);
        assertThat(claims.role()).isEqualTo("CUSTOMER");
    }

    @Test
    void 변조된_토큰은_UNAUTHORIZED() {
        JwtProvider provider = new JwtProvider(SECRET, 3600);
        String token = provider.createToken(1L, "CUSTOMER");

        // 서명 부분(마지막 '.' 뒤)의 첫 글자를 바꿔서 변조
        int signatureStart = token.lastIndexOf('.') + 1;
        char first = token.charAt(signatureStart);
        String tampered = token.substring(0, signatureStart)
            + (first == 'A' ? 'B' : 'A')
            + token.substring(signatureStart + 1);

        assertUnauthorized(provider, tampered);
    }

    @Test
    void 다른_키로_만든_토큰은_UNAUTHORIZED() {
        JwtProvider other = new JwtProvider("another-secret-key-must-be-32-bytes-long!!", 3600);
        String token = other.createToken(1L, "ADMIN");

        assertUnauthorized(new JwtProvider(SECRET, 3600), token);
    }

    @Test
    void 만료된_토큰은_UNAUTHORIZED() {
        // 만료 시간을 과거로 만들어 발급 즉시 만료된 토큰을 만든다
        JwtProvider expired = new JwtProvider(SECRET, -60);
        String token = expired.createToken(1L, "CUSTOMER");

        assertUnauthorized(expired, token);
    }

    @Test
    void 형식이_잘못된_토큰은_UNAUTHORIZED() {
        JwtProvider provider = new JwtProvider(SECRET, 3600);

        assertUnauthorized(provider, "not-a-jwt");
        assertUnauthorized(provider, "");
    }

    @Test
    void 키가_32바이트보다_짧으면_생성되지_않는다() {
        assertThatThrownBy(() -> new JwtProvider("short-secret", 3600))
            .isInstanceOf(WeakKeyException.class);
    }

    private void assertUnauthorized(JwtProvider provider, String token) {
        assertThatThrownBy(() -> provider.validate(token))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.UNAUTHORIZED);
    }
}
