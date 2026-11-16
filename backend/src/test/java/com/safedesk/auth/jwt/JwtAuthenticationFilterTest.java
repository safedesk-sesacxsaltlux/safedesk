package com.safedesk.auth.jwt;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

public class JwtAuthenticationFilterTest {

    private final JwtProvider jwtProvider = new JwtProvider("test-secret-key-must-be-at-least-32-bytes!!", 3600);
    private final TokenBlacklist tokenBlacklist = new TokenBlacklist();
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtProvider, tokenBlacklist);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 유효한_토큰이면_memberId와_권한을_등록한다() throws Exception {
        Authentication auth = runWith("Bearer " + jwtProvider.createToken(13L, "CUSTOMER"));

        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(13L);
        assertThat(auth.getName()).isEqualTo("13"); // WebSocket principal.getName()과 같은 값
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_CUSTOMER");
    }

    @Test
    void 헤더가_없으면_등록하지_않는다() throws Exception {
        assertThat(runWith(null)).isNull();
    }

    @Test
    void Bearer가_아닌_헤더는_무시한다() throws Exception {
        assertThat(runWith("Basic abc")).isNull();
    }

    @Test
    void 로그아웃한_토큰은_등록하지_않는다() throws Exception {
        String token = jwtProvider.createToken(13L, "CUSTOMER");
        tokenBlacklist.add(token, jwtProvider.validate(token).expiresAt());

        assertThat(runWith("Bearer " + token)).isNull();
    }

    @Test
    void 잘못된_토큰이면_등록하지_않고_요청은_계속_진행한다() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer not-a-jwt");

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull(); // 다음 필터로 넘어갔는지
    }

    private Authentication runWith(String authorizationHeader) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (authorizationHeader != null) {
            request.addHeader("Authorization", authorizationHeader);
        }
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
