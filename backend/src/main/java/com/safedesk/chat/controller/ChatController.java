package com.safedesk.chat.controller;

import com.safedesk.chat.dto.ChatQueueResponseDTO;
import com.safedesk.chat.service.ChatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chats")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("queue")
    public List<ChatQueueResponseDTO> getQueueList() {

        return chatService.getQueueList();

    }
}
