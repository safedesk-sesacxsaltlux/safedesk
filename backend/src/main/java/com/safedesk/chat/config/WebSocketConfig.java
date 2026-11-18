package com.safedesk.chat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker   // WebSocket 메시지 송수신 기능을 활성화
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    private final String[] allowedOrigins;

    // 브라우저는 WebSocket 연결 때 Origin 헤더를 보낸다. 기본값은 같은 출처만 허용이라 프론트(3000)에서 연결하면 403 이 난다.
    // REST CORS(SecurityConfig)와 같은 주소를 기본값으로 두고, 배포 때는 websocket.allowed-origins 로 프론트 주소를 넣는다.
    public WebSocketConfig(StompAuthChannelInterceptor stompAuthChannelInterceptor,
                           @Value("${websocket.allowed-origins:http://localhost:3000}") String[] allowedOrigins) {
        this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // registry : 연결 주소를 관리하는 객체, addEndpoint() : 연결 주소를 등록하는 메서드
        registry.addEndpoint("/ws").setAllowedOriginPatterns(allowedOrigins);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        /* setApplicationDestinationPrefixes() :
        클라이언트가 서버로 메시지를 보낼 때 사용할 주소의 공통 시작 부분(접두사)을 설정하는 메서드 */
        registry.setApplicationDestinationPrefixes("/app");

        /* enableSimpleBroker
        : 서버가 고객이나 상담원에게 메시지를 전달할 때 사용하는 경로를 설정
          /queue 는 본인에게만 가는 오류 알림(/user/queue/errors)용 */
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setUserDestinationPrefix("/user");
    }

    // 클라이언트가 보내는 모든 STOMP 프레임(CONNECT·SUBSCRIBE·SEND)을 인증·권한 검사한다 (NFR-004)
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
