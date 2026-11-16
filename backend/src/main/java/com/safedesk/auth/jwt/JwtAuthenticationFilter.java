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

// 토큰이 잘못돼도 여기서 막지 않는다. 막을지는 SecurityConfig의 경로 규칙이 정한다.
// @Component를 붙이면 일반 서블릿 필터로도 등록돼 두 번 실행되므로 SecurityConfig에서 직접 생성한다.
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final TokenBlacklist tokenBlacklist;

    public JwtAuthenticationFilter(JwtProvider jwtProvider, TokenBlacklist tokenBlacklist) {
        this.jwtProvider = jwtProvider;
        this.tokenBlacklist = tokenBlacklist;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {

        String token = BearerToken.resolve(request.getHeader("Authorization"));
        // TODO(인증): WebSocket STOMP CONNECT 인터셉터에도 같은 블랙리스트 확인 필요
        if (token != null && !tokenBlacklist.contains(token)) {
            try {
                JwtClaims claims = jwtProvider.validate(token);

                // principal을 memberId로 두면 principal.getName()이 memberId 문자열이 된다 (WebSocket에서도 같은 값).
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
