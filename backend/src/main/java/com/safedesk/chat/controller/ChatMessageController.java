package com.safedesk.chat.controller;

import com.safedesk.chat.dto.ChatErrorMessage;
import com.safedesk.chat.dto.ChatMessageRequestDTO;
import com.safedesk.chat.service.ChatService;
import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
public class ChatMessageController {

    private final ChatService chatService;

    public ChatMessageController(ChatService chatService) {
        this.chatService = chatService;
    }

    // 발신자는 CONNECT 때 토큰으로 정한 사용자(principal)로 정한다. 본문의 senderType·senderId 는 받지 않는다 (NFR-004)
    @MessageMapping("chats/{consultationId}/messages")
    // @Payload는 클라이언트가 보낸 메시지의 본문 데이터를 받아오는 어노테이션
    public void sendMessage(@DestinationVariable long consultationId,
                            @Payload ChatMessageRequestDTO message,
                            Principal principal) {

        chatService.sendMessage(consultationId, memberIdOf(principal), message.getContent());
    }

    // 보낸 사람에게만 오류를 돌려준다 (contract.md WebSocket 「오류」). 예: 종료된 상담 → CHAT_CLOSED
    @MessageExceptionHandler(BusinessException.class)
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public ChatErrorMessage handleBusiness(BusinessException e) {
        log.warn("WebSocket 비즈니스 예외: code={}, message={}", e.getErrorCode().name(), e.getMessage());
        return ChatErrorMessage.of(e.getErrorCode(), e.getMessage());
    }

    // 본문이 JSON 이 아니거나 형식이 맞지 않으면(예: {"content": 아님) 변환에 실패한다. 아무 안내 없이 버려지지 않게 INVALID_INPUT 으로 알린다
    @MessageExceptionHandler(MessageConversionException.class)
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public ChatErrorMessage handleInvalidPayload(MessageConversionException e) {
        log.warn("WebSocket 메시지 본문 변환 실패: {}", e.getMessage());
        return ChatErrorMessage.of(ErrorCode.INVALID_INPUT, ErrorCode.INVALID_INPUT.getMessage());
    }

    private Long memberIdOf(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth && auth.getPrincipal() instanceof Long memberId) {
            return memberId;
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
}
