package com.safedesk.chat.controller;

import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;

/**
 * 상담 API 공통 인증 확인.
 * SecurityConfig 가 아직 anyRequest().permitAll() 이라, 토큰이 없으면 익명 인증이 들어온다. 여기서 401·403 을 나눈다.
 */
final class ChatAuth {

    private static final String ROLE_AGENT = "ROLE_AGENT";

    private ChatAuth() {
    }

    // 로그인한 회원의 memberId. 로그인 안 했으면 401
    static Long memberId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long memberId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return memberId;
    }

    // 상담원만 쓰는 API. 상담원이 아니면 403 (체크리스트 E8)
    static Long agentId(Authentication authentication) {
        Long memberId = memberId(authentication);
        boolean isAgent = authentication.getAuthorities().stream()
            .anyMatch(a -> ROLE_AGENT.equals(a.getAuthority()));
        if (!isAgent) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return memberId;
    }
}
