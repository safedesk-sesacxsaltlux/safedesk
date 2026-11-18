package com.safedesk.chat.config;

import com.safedesk.auth.jwt.BearerToken;
import com.safedesk.auth.jwt.JwtClaims;
import com.safedesk.auth.jwt.JwtProvider;
import com.safedesk.auth.jwt.TokenBlacklist;
import com.safedesk.chat.dto.ChatErrorMessage;
import com.safedesk.chat.entity.Consultation;
import com.safedesk.chat.repository.ConsultationRepository;
import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * STOMP 인증·구독·전송 권한 (NFR-004, contract.md WebSocket 「인증」「보내기」「구독 권한」「오류」).
 * <p>
 * - CONNECT: 헤더 {@code Authorization: Bearer {token}} 을 검증하고, 세션 사용자(principal)를 memberId 로 정한다.
 *   브라우저 WebSocket 은 HTTP 헤더를 못 붙여서 STOMP CONNECT 프레임 헤더로 받는다.
 *   검증한 토큰과 만료 시각은 세션에 저장해 두고, 이후 SEND·SUBSCRIBE 때마다 다시 확인한다.
 * - SUBSCRIBE: {@code /topic/chats/{chatId}} 는 그 상담의 고객이나 수락한 상담원만 구독할 수 있다.
 *   없는 상담과 남의 상담은 똑같이 {@code CHAT_ACCESS_DENIED} 로 거부한다 (상담 번호 존재 여부를 드러내지 않음).
 *   {@code /user/queue/errors} 는 본인 오류 큐라 누구나 구독할 수 있다.
 * - SEND: {@code /app/chats/{chatId}/messages} 로만 보낼 수 있고, 그 상담의 고객이나 수락한 상담원이어야 한다.
 *   {@code /topic/**}, {@code /user/**}, {@code /queue/**} 처럼 브로커로 바로 가는 목적지는 모두 거부한다.
 *   (Spring 기본 브로커는 클라이언트가 /topic 으로 직접 보낸 메시지도 구독자에게 그대로 전달하기 때문)
 * <p>
 * 거부 방법
 * - CONNECT·SUBSCRIBE 의 경로·권한 오류: 예외를 던져 STOMP ERROR 프레임을 보내고 연결을 끊는다.
 * - CONNECT 이후 토큰이 로그아웃·만료됐거나 SEND 가 거부되면: 보낸 사람의 {@code /user/queue/errors} 로 알리고
 *   그 프레임만 버린다 (contract.md WebSocket 「오류」).
 */
@Slf4j
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern CHAT_TOPIC = Pattern.compile("^/topic/chats/(\\d+)$");
    private static final Pattern CHAT_SEND = Pattern.compile("^/app/chats/(\\d+)/messages$");
    private static final String ERROR_QUEUE = "/user/queue/errors";

    // CONNECT 때 검증한 토큰을 세션에 저장하는 키. 토큰에 jti 가 없고 블랙리스트가 토큰 문자열 기준이라 토큰 자체를 둔다
    static final String SESSION_TOKEN = "safedesk.accessToken";
    static final String SESSION_EXPIRES_AT = "safedesk.accessTokenExpiresAt";

    private final JwtProvider jwtProvider;
    private final TokenBlacklist tokenBlacklist;
    private final ConsultationRepository consultationRepository;
    // 메시지 브로커 설정이 이 인터셉터를 주입받기 때문에, 바로 주입하면 순환 참조가 된다. 쓸 때 꺼낸다
    private final ObjectProvider<SimpMessageSendingOperations> messagingTemplate;
    private final Clock clock;

    @Autowired
    public StompAuthChannelInterceptor(JwtProvider jwtProvider, TokenBlacklist tokenBlacklist,
                                       ConsultationRepository consultationRepository,
                                       ObjectProvider<SimpMessageSendingOperations> messagingTemplate) {
        this(jwtProvider, tokenBlacklist, consultationRepository, messagingTemplate, Clock.systemUTC());
    }

    // 테스트에서 시간을 고정하기 위한 생성자
    StompAuthChannelInterceptor(JwtProvider jwtProvider, TokenBlacklist tokenBlacklist,
                                ConsultationRepository consultationRepository,
                                ObjectProvider<SimpMessageSendingOperations> messagingTemplate, Clock clock) {
        this.jwtProvider = jwtProvider;
        this.tokenBlacklist = tokenBlacklist;
        this.consultationRepository = consultationRepository;
        this.messagingTemplate = messagingTemplate;
        this.clock = clock;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;   // 하트비트 등
        }

        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command)) {
            accessor.setUser(authenticate(accessor));
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            Long memberId = memberIdOf(accessor.getUser());
            if (!tokenStillValid(accessor, memberId)) {
                return null;
            }
            checkSubscribe(memberId, accessor.getDestination());
        } else if (StompCommand.SEND.equals(command)) {
            Long memberId = memberIdOf(accessor.getUser());
            if (!tokenStillValid(accessor, memberId)) {
                return null;
            }
            ErrorCode denied = checkSend(memberId, accessor.getDestination());
            if (denied != null) {
                notifyError(accessor, memberId, denied);
                return null;
            }
        }
        return message;
    }

    private UsernamePasswordAuthenticationToken authenticate(StompHeaderAccessor accessor) {
        String token = BearerToken.resolve(accessor.getFirstNativeHeader("Authorization"));
        if (token == null || tokenBlacklist.contains(token)) {
            throw new MessageDeliveryException(ErrorCode.UNAUTHORIZED.name());
        }

        JwtClaims claims;
        try {
            claims = jwtProvider.validate(token);
        } catch (BusinessException e) {
            throw new MessageDeliveryException(ErrorCode.UNAUTHORIZED.name());
        }

        // 이후 SEND·SUBSCRIBE 때 로그아웃·만료를 다시 확인할 수 있게 세션에 남긴다
        Map<String, Object> session = accessor.getSessionAttributes();
        if (session != null) {
            session.put(SESSION_TOKEN, token);
            session.put(SESSION_EXPIRES_AT, claims.expiresAt());
        }

        // HTTP 필터(JwtAuthenticationFilter)와 같은 모양. principal.getName() 이 memberId 문자열이 된다
        return new UsernamePasswordAuthenticationToken(
            claims.memberId(), null, List.of(new SimpleGrantedAuthority("ROLE_" + claims.role())));
    }

    // CONNECT 때 검증한 토큰이 그 뒤에 로그아웃(블랙리스트)되거나 만료됐으면 오류 큐로 알리고 false
    private boolean tokenStillValid(StompHeaderAccessor accessor, Long memberId) {
        Map<String, Object> session = accessor.getSessionAttributes();
        Object token = session == null ? null : session.get(SESSION_TOKEN);
        Object expiresAt = session == null ? null : session.get(SESSION_EXPIRES_AT);

        boolean valid = token instanceof String t
            && expiresAt instanceof Instant exp
            && exp.isAfter(clock.instant())
            && !tokenBlacklist.contains(t);
        if (!valid) {
            notifyError(accessor, memberId, ErrorCode.UNAUTHORIZED);
        }
        return valid;
    }

    private void checkSubscribe(Long memberId, String destination) {
        if (ERROR_QUEUE.equals(destination)) {
            return;
        }

        Matcher matcher = destination == null ? null : CHAT_TOPIC.matcher(destination);
        if (matcher == null || !matcher.matches()) {
            throw new MessageDeliveryException(ErrorCode.ACCESS_DENIED.name());
        }

        ErrorCode denied = checkParticipant(memberId, Long.parseLong(matcher.group(1)));
        if (denied != null) {
            throw new MessageDeliveryException(denied.name());
        }
    }

    // 허용하면 null, 거부하면 오류 코드
    private ErrorCode checkSend(Long memberId, String destination) {
        Matcher matcher = destination == null ? null : CHAT_SEND.matcher(destination);
        if (matcher == null || !matcher.matches()) {
            return ErrorCode.ACCESS_DENIED;
        }
        return checkParticipant(memberId, Long.parseLong(matcher.group(1)));
    }

    // 상담의 고객이거나 수락한 상담원이면 null.
    // 없는 상담도 남의 상담과 같은 CHAT_ACCESS_DENIED 로 돌려줘서, 응답으로 상담 번호가 있는지 알 수 없게 한다 (PR #80 리뷰).
    // REST(API-003·004·006 등)는 계약(contract.md, customer-chat.md)대로 404 CHAT_NOT_FOUND 를 유지한다.
    private ErrorCode checkParticipant(Long memberId, long consultationId) {
        Consultation consultation = consultationRepository.findById(consultationId).orElse(null);
        if (consultation == null) {
            return ErrorCode.CHAT_ACCESS_DENIED;
        }

        boolean isCustomer = memberId.equals(consultation.getCustomerId());
        boolean isAssignedAgent = Objects.equals(memberId, consultation.getAgentId());
        return isCustomer || isAssignedAgent ? null : ErrorCode.CHAT_ACCESS_DENIED;
    }

    // 보낸 연결(세션)에만 오류를 보낸다. ChatMessageController 의 @SendToUser(broadcast = false) 와 같은 방식
    private void notifyError(StompHeaderAccessor accessor, Long memberId, ErrorCode errorCode) {
        log.warn("WebSocket {} 거부: memberId={}, destination={}, code={}",
            accessor.getCommand(), memberId, accessor.getDestination(), errorCode.name());

        SimpMessageSendingOperations template = messagingTemplate.getIfAvailable();
        if (template == null || accessor.getSessionId() == null) {
            return;
        }
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headers.setSessionId(accessor.getSessionId());
        headers.setLeaveMutable(true);
        template.convertAndSendToUser(String.valueOf(memberId), "/queue/errors",
            ChatErrorMessage.of(errorCode, errorCode.getMessage()), headers.getMessageHeaders());
    }

    // CONNECT 때 정한 사용자에서 memberId 를 꺼낸다. CONNECT 인증을 거치지 않았으면 거부
    static Long memberIdOf(Principal user) {
        if (user instanceof UsernamePasswordAuthenticationToken auth && auth.getPrincipal() instanceof Long memberId) {
            return memberId;
        }
        throw new MessageDeliveryException(ErrorCode.UNAUTHORIZED.name());
    }
}
