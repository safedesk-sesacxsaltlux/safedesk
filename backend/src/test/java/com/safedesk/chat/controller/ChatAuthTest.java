package com.safedesk.chat.controller;

import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatAuthTest {

    @Test
    @DisplayName("상담원 토큰이면 memberId 를 돌려준다")
    void agent() {
        assertThat(ChatAuth.agentId(token(10L, "AGENT"))).isEqualTo(10L);
    }

    @Test
    @DisplayName("고객 토큰으로 상담원 API 를 부르면 403 (체크리스트 E8)")
    void customerOnAgentApi() {
        assertThatThrownBy(() -> ChatAuth.agentId(token(100L, "CUSTOMER")))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("로그인하지 않았으면 401")
    void anonymous() {
        AnonymousAuthenticationToken anonymous = new AnonymousAuthenticationToken(
            "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

        assertThatThrownBy(() -> ChatAuth.memberId(anonymous))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.UNAUTHORIZED);
        assertThatThrownBy(() -> ChatAuth.memberId(null))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    private static UsernamePasswordAuthenticationToken token(long memberId, String role) {
        return new UsernamePasswordAuthenticationToken(memberId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
}
