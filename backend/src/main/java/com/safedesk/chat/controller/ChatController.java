
package com.safedesk.chat.controller;

import com.safedesk.chat.dto.ChatQueueResponseDTO;
import com.safedesk.chat.dto.ChatAcceptResponseDTO;
import com.safedesk.chat.dto.ChatCloseResponseDTO;
import com.safedesk.chat.dto.ChatHistoryResponseDTO;
import com.safedesk.chat.service.ChatService;
import com.safedesk.chat.service.ChatHistoryService;
import com.safedesk.common.response.ApiResponse;
import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
public class ChatController {

    private final ChatService chatService;
    private final ChatHistoryService chatHistoryService;

    public ChatController(
        ChatService chatService,
        ChatHistoryService chatHistoryService
    ) {
        this.chatService = chatService;
        this.chatHistoryService = chatHistoryService;
    }

    @GetMapping("queue")
    public ResponseEntity<ApiResponse<List<ChatQueueResponseDTO>>> getQueueList() {

        return ResponseEntity.ok(ApiResponse.success(
            chatService.getQueueList(), "조회되었습니다."));
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

    // API-006 상담 종료 (상담원)
    @PatchMapping("/{consultationId}/close")
    public ResponseEntity<ApiResponse<ChatCloseResponseDTO>> closeConsultation(
        @PathVariable long consultationId,
        @AuthenticationPrincipal Long agentId) {

        if (agentId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return ResponseEntity.ok(ApiResponse.success(
            chatService.closeConsultation(consultationId, agentId),
            "상담이 종료되었습니다."));
    }

    // 상담 메시지 내역 조회
    @GetMapping("/{consultationId}/messages")
    public ResponseEntity<ApiResponse<List<ChatHistoryResponseDTO>>> getChatHistory(
        @PathVariable long consultationId,
        @AuthenticationPrincipal Long agentId) {

        if (agentId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return ResponseEntity.ok(ApiResponse.success(
            chatHistoryService.getChatHistory(consultationId, agentId),
            "조회되었습니다."));
    }
}
