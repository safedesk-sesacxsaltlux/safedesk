package com.safedesk.chat.controller;

import com.safedesk.chat.dto.ChatMessageRequestDTO;
import com.safedesk.chat.service.ChatService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
public class ChatMessageController {

    private final ChatService chatService;

    @MessageMapping("chats/{chatId}/messages")
    // @Payload는 클라이언트가 보낸 메시지의 본문 데이터를 받아오는 어노테이션
    public void sendMessage(@Payload ChatMessageRequestDTO message) {
        chatService.sendMessage(message);
    }

    public ChatMessageController(ChatService chatService) {
        this.chatService = chatService;
    }
}
