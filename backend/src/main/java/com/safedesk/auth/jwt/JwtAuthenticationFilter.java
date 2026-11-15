package com.safedesk.auth.jwt;

import com.safedesk.common.exception.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// Authorization: Bearer {token} 헤더가 있으면 검증해서 "현재 로그인한 사람"으로 등록한다.
// 토큰이 없거나 잘못돼도 여기서 막지 않는다. 막을지는 SecurityConfig의 경로 규칙이 정하고,
// 인증이 필요한 경로면 JwtAuthenticationEntryPoint가 401로 응답한다.
// @Component를 붙이면 Spring Boot가 일반 서블릿 필터로도 등록해서 두 번 실행되므로 SecurityConfig에서 직접 생성한다.
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    public JwtAuthenticationFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                JwtClaims claims = jwtProvider.validate(token);

                // principal에 memberId를 넣으면 컨트롤러에서 @AuthenticationPrincipal Long memberId로 받고,
                // principal.getName()은 memberId 문자열("13")을 돌려준다.
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    claims.memberId(),
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + claims.role()))
                );
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            } catch (BusinessException e) {
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }
}
