package com.safedesk.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker   // WebSocket 메시지 송수신 기능을 활성화
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // registry : 연결 주소를 관리하는 객체, addEndpoint() : 연결 주소를 등록하는 메서드
        registry.addEndpoint("/ws");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        /* setApplicationDestinationPrefixes() :
        클라이언트가 서버로 메시지를 보낼 때 사용할 주소의 공통 시작 부분(접두사)을 설정하는 메서드 */
        registry.setApplicationDestinationPrefixes("/app");

        /* enableSimpleBroker
        : 서버가 고객이나 상담원에게 메시지를 전달할 때 사용하는 경로를 설정 */
        registry.enableSimpleBroker("/topic");
    }
}
