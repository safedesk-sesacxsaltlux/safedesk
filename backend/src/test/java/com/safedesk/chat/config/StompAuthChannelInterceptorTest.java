package com.safedesk.chat.config;

import com.safedesk.auth.jwt.JwtProvider;
import com.safedesk.auth.jwt.TokenBlacklist;
import com.safedesk.chat.entity.Consultation;
import com.safedesk.chat.repository.ConsultationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StompAuthChannelInterceptorTest {

    private final JwtProvider jwtProvider = new JwtProvider("test-secret-key-must-be-at-least-32-bytes!!", 3600);
    private final TokenBlacklist tokenBlacklist = new TokenBlacklist();
    private final ConsultationRepository consultationRepository = mock(ConsultationRepository.class);
    private final StompAuthChannelInterceptor interceptor =
        new StompAuthChannelInterceptor(jwtProvider, tokenBlacklist, consultationRepository);
    private final MessageChannel channel = mock(MessageChannel.class);

    // ===== CONNECT =====

    @Test
    @DisplayName("CONNECT 토큰이 유효하면 memberId 를 연결 사용자로 정한다")
    void connectWithValidToken() {
        Message<byte[]> message = connect("Bearer " + jwtProvider.createToken(100L, "CUSTOMER"));

        interceptor.preSend(message, channel);

        Principal user = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class).getUser();
        assertThat(user).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(((UsernamePasswordAuthenticationToken) user).getPrincipal()).isEqualTo(100L);
        assertThat(user.getName()).isEqualTo("100");
    }

    @Test
    @DisplayName("CONNECT 에 토큰이 없거나 잘못됐으면 연결을 거부한다")
    void connectWithoutValidToken() {
        assertThatThrownBy(() -> interceptor.preSend(connect(null), channel))
            .isInstanceOf(MessageDeliveryException.class);
        assertThatThrownBy(() -> interceptor.preSend(connect("Bearer invalid.token.value"), channel))
            .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    @DisplayName("로그아웃한 토큰으로는 연결할 수 없다")
    void connectWithBlacklistedToken() {
        String token = jwtProvider.createToken(100L, "CUSTOMER");
        tokenBlacklist.add(token, Instant.now().plusSeconds(3600));

        assertThatThrownBy(() -> interceptor.preSend(connect("Bearer " + token), channel))
            .isInstanceOf(MessageDeliveryException.class);
    }

    // ===== SEND =====

    @Test
    @DisplayName("CONNECT 인증을 거치지 않은 연결은 메시지를 보낼 수 없다")
    void sendWithoutUser() {
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SEND, "/app/chats/1/messages", null), channel))
            .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    @DisplayName("인증된 연결은 메시지를 보낼 수 있다 (상담 참여자 확인은 ChatService)")
    void sendWithUser() {
        Message<byte[]> message = frame(StompCommand.SEND, "/app/chats/1/messages", user(100L));
        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    }

    // ===== SUBSCRIBE =====

    @Test
    @DisplayName("상담의 고객과 수락한 상담원은 구독할 수 있다")
    void subscribeAsParticipant() {
        givenConsultation(1L, 100L, 10L);

        interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/chats/1", user(100L)), channel);
        interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/chats/1", user(10L)), channel);
    }

    @Test
    @DisplayName("대기 중(상담원 없음) 상담도 고객은 구독할 수 있다 (수락 알림을 받기 위해)")
    void subscribeQueuedAsCustomer() {
        givenConsultation(1L, 100L, null);

        interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/chats/1", user(100L)), channel);
    }

    @Test
    @DisplayName("다른 사람의 상담은 구독할 수 없다")
    void subscribeAsOutsider() {
        givenConsultation(1L, 100L, 10L);

        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/chats/1", user(999L)), channel))
            .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    @DisplayName("없는 상담이나 허용하지 않은 경로는 구독할 수 없다")
    void subscribeUnknownDestination() {
        when(consultationRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/chats/2", user(100L)), channel))
            .isInstanceOf(MessageDeliveryException.class);
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/chats/*", user(100L)), channel))
            .isInstanceOf(MessageDeliveryException.class);
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/other", user(100L)), channel))
            .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    @DisplayName("본인 오류 큐는 구독할 수 있고, 인증 안 된 연결은 구독할 수 없다")
    void subscribeErrorQueue() {
        interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/user/queue/errors", user(100L)), channel);

        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/user/queue/errors", null), channel))
            .isInstanceOf(MessageDeliveryException.class);
    }

    // ===== 준비 =====

    private void givenConsultation(long consultationId, long customerId, Long agentId) {
        Consultation consultation = mock(Consultation.class);
        when(consultation.getCustomerId()).thenReturn(customerId);
        when(consultation.getAgentId()).thenReturn(agentId);
        when(consultationRepository.findById(consultationId)).thenReturn(Optional.of(consultation));
    }

    private static UsernamePasswordAuthenticationToken user(long memberId) {
        return new UsernamePasswordAuthenticationToken(memberId, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    private static Message<byte[]> connect(String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authorization != null) {
            accessor.addNativeHeader("Authorization", authorization);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private static Message<byte[]> frame(StompCommand command, String destination, Principal user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        if (user != null) {
            accessor.setUser(user);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
