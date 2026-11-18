package com.safedesk.chat.config;

import com.safedesk.auth.jwt.BearerToken;
import com.safedesk.auth.jwt.JwtClaims;
import com.safedesk.auth.jwt.JwtProvider;
import com.safedesk.auth.jwt.TokenBlacklist;
import com.safedesk.chat.entity.Consultation;
import com.safedesk.chat.repository.ConsultationRepository;
import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * STOMP 인증·구독 권한 (NFR-004, contract.md WebSocket 「인증」「구독 권한」).
 * <p>
 * - CONNECT: 헤더 {@code Authorization: Bearer {token}} 을 검증하고, 세션 사용자(principal)를 memberId 로 정한다.
 *   브라우저 WebSocket 은 HTTP 헤더를 못 붙여서 STOMP CONNECT 프레임 헤더로 받는다.
 *   여기서 정한 사용자는 같은 연결의 SEND·SUBSCRIBE 에 그대로 따라간다.
 * - SUBSCRIBE: {@code /topic/chats/{chatId}} 는 그 상담의 고객이나 수락한 상담원만 구독할 수 있다.
 *   {@code /user/queue/errors} 는 본인 오류 큐라 누구나 구독할 수 있다.
 * - SEND: 로그인한 연결만 보낼 수 있다. 상담 참여자인지는 ChatService 가 확인한다.
 * <p>
 * 여기서 예외를 던지면 Spring 이 STOMP ERROR 프레임을 보내고 연결을 끊는다.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern CHAT_TOPIC = Pattern.compile("^/topic/chats/(\\d+)$");
    private static final String ERROR_QUEUE = "/user/queue/errors";

    private final JwtProvider jwtProvider;
    private final TokenBlacklist tokenBlacklist;
    private final ConsultationRepository consultationRepository;

    public StompAuthChannelInterceptor(JwtProvider jwtProvider, TokenBlacklist tokenBlacklist,
                                       ConsultationRepository consultationRepository) {
        this.jwtProvider = jwtProvider;
        this.tokenBlacklist = tokenBlacklist;
        this.consultationRepository = consultationRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;   // 하트비트 등
        }

        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command)) {
            accessor.setUser(authenticate(accessor.getFirstNativeHeader("Authorization")));
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            checkSubscribe(memberIdOf(accessor.getUser()), accessor.getDestination());
        } else if (StompCommand.SEND.equals(command)) {
            memberIdOf(accessor.getUser());   // 로그인 확인만. 상담 참여자 확인은 ChatService
        }
        return message;
    }

    private UsernamePasswordAuthenticationToken authenticate(String authorizationHeader) {
        String token = BearerToken.resolve(authorizationHeader);
        if (token == null || tokenBlacklist.contains(token)) {
            throw new MessageDeliveryException(ErrorCode.UNAUTHORIZED.name());
        }

        JwtClaims claims;
        try {
            claims = jwtProvider.validate(token);
        } catch (BusinessException e) {
            throw new MessageDeliveryException(ErrorCode.UNAUTHORIZED.name());
        }

        // HTTP 필터(JwtAuthenticationFilter)와 같은 모양. principal.getName() 이 memberId 문자열이 된다
        return new UsernamePasswordAuthenticationToken(
            claims.memberId(), null, List.of(new SimpleGrantedAuthority("ROLE_" + claims.role())));
    }

    private void checkSubscribe(Long memberId, String destination) {
        if (ERROR_QUEUE.equals(destination)) {
            return;
        }

        Matcher matcher = destination == null ? null : CHAT_TOPIC.matcher(destination);
        if (matcher == null || !matcher.matches()) {
            throw new MessageDeliveryException(ErrorCode.ACCESS_DENIED.name());
        }

        long consultationId = Long.parseLong(matcher.group(1));
        Consultation consultation = consultationRepository.findById(consultationId)
            .orElseThrow(() -> new MessageDeliveryException(ErrorCode.CHAT_NOT_FOUND.name()));

        boolean isCustomer = memberId.equals(consultation.getCustomerId());
        boolean isAssignedAgent = Objects.equals(memberId, consultation.getAgentId());
        if (!isCustomer && !isAssignedAgent) {
            throw new MessageDeliveryException(ErrorCode.CHAT_ACCESS_DENIED.name());
        }
    }

    // CONNECT 때 정한 사용자에서 memberId 를 꺼낸다. CONNECT 인증을 거치지 않았으면 거부
    static Long memberIdOf(Principal user) {
        if (user instanceof UsernamePasswordAuthenticationToken auth && auth.getPrincipal() instanceof Long memberId) {
            return memberId;
        }
        throw new MessageDeliveryException(ErrorCode.UNAUTHORIZED.name());
    }
}
