
package com.safedesk.chat.controller;

import com.safedesk.chat.dto.ChatQueueResponseDTO;
import com.safedesk.chat.service.ChatService;
import com.safedesk.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.core.Authentication;
import java.util.List;
import com.safedesk.chat.dto.ChatAcceptResponseDTO;

@RestController
@RequestMapping("/api/chats")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("queue")
    public ResponseEntity<ApiResponse<List<ChatQueueResponseDTO>>> getQueueList() {

        return ResponseEntity.ok(ApiResponse.success(chatService.getQueueList(), "조회되었습니다."));
    }

    @PatchMapping("/{consultationId}/accept")
    public ResponseEntity<ApiResponse<ChatAcceptResponseDTO>> acceptConsultation(
        @PathVariable long consultationId,
        Authentication authentication) {

        Long agentId = (Long) authentication.getPrincipal();

        return ResponseEntity.ok(ApiResponse.success(
            chatService.acceptConsultation(consultationId, agentId),
            "상담이 수락되었습니다."));
    }

}
