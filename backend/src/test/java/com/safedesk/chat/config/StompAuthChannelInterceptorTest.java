package com.safedesk.chat.config;

import com.safedesk.auth.jwt.JwtProvider;
import com.safedesk.auth.jwt.TokenBlacklist;
import com.safedesk.chat.dto.ChatErrorMessage;
import com.safedesk.chat.entity.Consultation;
import com.safedesk.chat.repository.ConsultationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StompAuthChannelInterceptorTest {

    private final JwtProvider jwtProvider = new JwtProvider("test-secret-key-must-be-at-least-32-bytes!!", 3600);
    private final TokenBlacklist tokenBlacklist = new TokenBlacklist();
    private final ConsultationRepository consultationRepository = mock(ConsultationRepository.class);
    private final SimpMessageSendingOperations messagingTemplate = mock(SimpMessageSendingOperations.class);
    private final MutableClock clock = new MutableClock(Instant.now());
    private final StompAuthChannelInterceptor interceptor = new StompAuthChannelInterceptor(
        jwtProvider, tokenBlacklist, consultationRepository, providerOf(messagingTemplate), clock);
    private final MessageChannel channel = mock(MessageChannel.class);

    private static final String SESSION_ID = "session-1";
    // 한 WebSocket 연결의 세션 속성. 실제로는 Spring 이 프레임마다 같은 Map 을 넣어 준다
    private final Map<String, Object> session = new HashMap<>();

    StompAuthChannelInterceptorTest() {
        // 기본은 CONNECT 를 거친 연결 (아직 유효한 토큰이 세션에 있음)
        session.put(StompAuthChannelInterceptor.SESSION_TOKEN, jwtProvider.createToken(100L, "CUSTOMER"));
        session.put(StompAuthChannelInterceptor.SESSION_EXPIRES_AT, clock.instant().plusSeconds(3600));
    }

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
    @DisplayName("CONNECT 때 검증한 토큰과 만료 시각을 세션에 저장한다")
    void connectStoresTokenInSession() {
        session.clear();
        String token = jwtProvider.createToken(100L, "CUSTOMER");

        interceptor.preSend(connect("Bearer " + token), channel);

        assertThat(session.get(StompAuthChannelInterceptor.SESSION_TOKEN)).isEqualTo(token);
        assertThat(session.get(StompAuthChannelInterceptor.SESSION_EXPIRES_AT)).isEqualTo(jwtProvider.validate(token).expiresAt());
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
    @DisplayName("상담의 고객과 수락한 상담원은 /app/chats/{id}/messages 로 메시지를 보낼 수 있다")
    void sendAsParticipant() {
        givenConsultation(1L, 100L, 10L);

        Message<byte[]> fromCustomer = frame(StompCommand.SEND, "/app/chats/1/messages", user(100L));
        Message<byte[]> fromAgent = frame(StompCommand.SEND, "/app/chats/1/messages", user(10L));

        assertThat(interceptor.preSend(fromCustomer, channel)).isSameAs(fromCustomer);
        assertThat(interceptor.preSend(fromAgent, channel)).isSameAs(fromAgent);
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any(), anyMap());
    }

    @Test
    @DisplayName("다른 사람의 상담으로는 보낼 수 없다 → 버리고 오류 큐로 CHAT_ACCESS_DENIED")
    void sendToOtherConsultation() {
        givenConsultation(2L, 200L, 20L);

        Message<byte[]> message = frame(StompCommand.SEND, "/app/chats/2/messages", user(100L));

        assertThat(interceptor.preSend(message, channel)).isNull();
        assertThat(notifiedErrorCode("100")).isEqualTo("CHAT_ACCESS_DENIED");
    }

    @Test
    @DisplayName("없는 상담으로는 보낼 수 없다 → 남의 상담과 같은 CHAT_ACCESS_DENIED (상담 번호 존재 여부를 드러내지 않음)")
    void sendToUnknownConsultation() {
        when(consultationRepository.findById(3L)).thenReturn(Optional.empty());

        assertThat(interceptor.preSend(frame(StompCommand.SEND, "/app/chats/3/messages", user(100L)), channel)).isNull();
        assertThat(notifiedErrorCode("100")).isEqualTo("CHAT_ACCESS_DENIED");
    }

    @Test
    @DisplayName("브로커 경로(/topic/chats/{id})로 직접 보내 가짜 CONSULTATION_CLOSED 를 뿌릴 수 없다 (상담 참여자여도)")
    void sendDirectlyToTopic() {
        givenConsultation(1L, 100L, 10L);

        Message<byte[]> fake = frame(StompCommand.SEND, "/topic/chats/1", user(100L),
            "{\"type\":\"CONSULTATION_CLOSED\",\"chatId\":1}");

        assertThat(interceptor.preSend(fake, channel)).isNull();
        assertThat(notifiedErrorCode("100")).isEqualTo("ACCESS_DENIED");
    }

    @Test
    @DisplayName("/app/chats/{id}/messages 가 아닌 목적지로는 보낼 수 없다")
    void sendToDisallowedDestinations() {
        givenConsultation(1L, 100L, 10L);

        for (String destination : List.of("/topic/chats/2", "/topic/other", "/user/queue/errors", "/user/10/queue/errors",
            "/queue/errors", "/app/chats/1", "/app/chats/1/messages/x", "/app/chats/abc/messages", "/app/other")) {
            assertThat(interceptor.preSend(frame(StompCommand.SEND, destination, user(100L)), channel))
                .as(destination).isNull();
        }
        Message<byte[]> noDestination = frame(StompCommand.SEND, null, user(100L));
        assertThat(interceptor.preSend(noDestination, channel)).isNull();
    }

    // ===== CONNECT 이후 토큰 재확인 =====

    @Test
    @DisplayName("CONNECT 뒤 로그아웃하면 그 연결로 SEND 할 수 없다 → 버리고 오류 큐로 UNAUTHORIZED")
    void sendAfterLogout() {
        givenConsultation(1L, 100L, 10L);
        String token = jwtProvider.createToken(100L, "CUSTOMER");
        interceptor.preSend(connect("Bearer " + token), channel);

        tokenBlacklist.add(token, jwtProvider.validate(token).expiresAt());   // 로그아웃 (API-AUTH-04)

        assertThat(interceptor.preSend(frame(StompCommand.SEND, "/app/chats/1/messages", user(100L)), channel)).isNull();
        assertThat(notifiedErrorCode("100")).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("CONNECT 뒤 토큰이 만료되면 그 연결로 SEND 할 수 없다 → 버리고 오류 큐로 UNAUTHORIZED")
    void sendAfterExpiry() {
        givenConsultation(1L, 100L, 10L);
        interceptor.preSend(connect("Bearer " + jwtProvider.createToken(100L, "CUSTOMER")), channel);
        Message<byte[]> beforeExpiry = frame(StompCommand.SEND, "/app/chats/1/messages", user(100L));
        assertThat(interceptor.preSend(beforeExpiry, channel)).isSameAs(beforeExpiry);

        clock.advanceSeconds(3601);   // jwt.expiration-seconds(3600) 경과

        assertThat(interceptor.preSend(frame(StompCommand.SEND, "/app/chats/1/messages", user(100L)), channel)).isNull();
        assertThat(notifiedErrorCode("100")).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("CONNECT 뒤 로그아웃·만료되면 새 구독도 할 수 없다 → 버리고 오류 큐로 UNAUTHORIZED")
    void subscribeAfterLogoutOrExpiry() {
        givenConsultation(1L, 100L, 10L);
        String token = jwtProvider.createToken(100L, "CUSTOMER");
        interceptor.preSend(connect("Bearer " + token), channel);
        tokenBlacklist.add(token, jwtProvider.validate(token).expiresAt());

        assertThat(interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/chats/1", user(100L)), channel)).isNull();
        assertThat(notifiedErrorCode("100")).isEqualTo("UNAUTHORIZED");

        session.put(StompAuthChannelInterceptor.SESSION_TOKEN, jwtProvider.createToken(100L, "CUSTOMER"));
        session.put(StompAuthChannelInterceptor.SESSION_EXPIRES_AT, clock.instant().minusSeconds(1));
        assertThat(interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/chats/1", user(100L)), channel)).isNull();
    }

    @Test
    @DisplayName("세션에 검증한 토큰이 없으면 SEND 할 수 없다")
    void sendWithoutSessionToken() {
        givenConsultation(1L, 100L, 10L);
        session.clear();

        assertThat(interceptor.preSend(frame(StompCommand.SEND, "/app/chats/1/messages", user(100L)), channel)).isNull();
        assertThat(notifiedErrorCode("100")).isEqualTo("UNAUTHORIZED");
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
    @DisplayName("없는 상담이나 허용하지 않은 경로는 구독할 수 없다 (없는 상담은 남의 상담과 같은 CHAT_ACCESS_DENIED)")
    void subscribeUnknownDestination() {
        when(consultationRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/chats/2", user(100L)), channel))
            .isInstanceOf(MessageDeliveryException.class)
            .hasMessage("CHAT_ACCESS_DENIED");
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

    @SuppressWarnings("unchecked")
    private static ObjectProvider<SimpMessageSendingOperations> providerOf(SimpMessageSendingOperations template) {
        ObjectProvider<SimpMessageSendingOperations> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(template);
        return provider;
    }

    // 보낸 연결(세션)에만 오류를 보냈는지 확인하고 errorCode 를 돌려준다
    @SuppressWarnings("unchecked")
    private String notifiedErrorCode(String user) {
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Map<String, Object>> headers = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSendToUser(eq(user), eq("/queue/errors"), payload.capture(), headers.capture());
        assertThat(headers.getValue().get(SimpMessageHeaderAccessor.SESSION_ID_HEADER)).isEqualTo(SESSION_ID);
        return ((ChatErrorMessage) payload.getValue()).getErrorCode();
    }

    private void givenConsultation(long consultationId, long customerId, Long agentId) {
        Consultation consultation = mock(Consultation.class);
        when(consultation.getCustomerId()).thenReturn(customerId);
        when(consultation.getAgentId()).thenReturn(agentId);
        when(consultationRepository.findById(consultationId)).thenReturn(Optional.of(consultation));
    }

    private static UsernamePasswordAuthenticationToken user(long memberId) {
        return new UsernamePasswordAuthenticationToken(memberId, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    private Message<byte[]> connect(String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authorization != null) {
            accessor.addNativeHeader("Authorization", authorization);
        }
        accessor.setSessionId(SESSION_ID);
        accessor.setSessionAttributes(session);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> frame(StompCommand command, String destination, Principal user) {
        return frame(command, destination, user, "");
    }

    private Message<byte[]> frame(StompCommand command, String destination, Principal user, String body) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (destination != null) {
            accessor.setDestination(destination);
        }
        if (user != null) {
            accessor.setUser(user);
        }
        accessor.setSessionId(SESSION_ID);
        accessor.setSessionAttributes(session);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(body.getBytes(StandardCharsets.UTF_8), accessor.getMessageHeaders());
    }

    // 만료 테스트용 시계
    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advanceSeconds(long seconds) {
            now = now.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
