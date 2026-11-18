package com.safedesk.chat.controller;

import com.safedesk.chat.dto.ChatAcceptResponseDTO;
import com.safedesk.chat.dto.ChatCloseResponseDTO;
import com.safedesk.chat.dto.ChatDetailResponseDTO;
import com.safedesk.chat.dto.ChatHistoryResponseDTO;
import com.safedesk.chat.dto.ChatQueueResponseDTO;
import com.safedesk.chat.service.ChatHistoryService;
import com.safedesk.chat.service.ChatService;
import com.safedesk.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    // API-001 상담 대기열 (상담원)
    @GetMapping("queue")
    public ResponseEntity<ApiResponse<List<ChatQueueResponseDTO>>> getQueueList(Authentication authentication) {
        ChatAuth.agentId(authentication);

        return ResponseEntity.ok(ApiResponse.success(
            chatService.getQueueList(), "조회되었습니다."));
    }

    // API-002 상담 수락 (상담원)
    @PatchMapping("/{chatId}/accept")
    public ResponseEntity<ApiResponse<ChatAcceptResponseDTO>> acceptConsultation(
        @PathVariable long chatId,
        Authentication authentication) {

        Long agentId = ChatAuth.agentId(authentication);

        return ResponseEntity.ok(ApiResponse.success(
            chatService.acceptConsultation(chatId, agentId),
            "상담이 수락되었습니다."));
    }

    // API-003 상담 상세 (해당 상담의 고객·수락한 상담원)
    @GetMapping("/{chatId}")
    public ResponseEntity<ApiResponse<ChatDetailResponseDTO>> getChatDetail(
        @PathVariable long chatId,
        Authentication authentication) {

        return ResponseEntity.ok(ApiResponse.success(
            chatHistoryService.getChatDetail(chatId, ChatAuth.memberId(authentication)),
            "조회되었습니다."));
    }

    // API-006 상담 종료 (수락한 상담원 또는 상담 중인 고객)
    @PatchMapping("/{chatId}/close")
    public ResponseEntity<ApiResponse<ChatCloseResponseDTO>> closeConsultation(
        @PathVariable long chatId,
        Authentication authentication) {

        return ResponseEntity.ok(ApiResponse.success(
            chatService.closeConsultation(chatId, ChatAuth.memberId(authentication)),
            "상담이 종료되었습니다."));
    }

    // API-004 상담 메시지 내역 (해당 상담의 고객·수락한 상담원)
    @GetMapping("/{chatId}/messages")
    public ResponseEntity<ApiResponse<List<ChatHistoryResponseDTO>>> getChatHistory(
        @PathVariable long chatId,
        Authentication authentication) {

        return ResponseEntity.ok(ApiResponse.success(
            chatHistoryService.getChatHistory(chatId, ChatAuth.memberId(authentication)),
            "조회되었습니다."));
    }

    // API-007 AI 응대 내역 (수락한 상담원)
    @GetMapping("/{chatId}/ai-messages")
    public ResponseEntity<ApiResponse<List<ChatHistoryResponseDTO>>> getAiMessages(
        @PathVariable long chatId,
        Authentication authentication) {

        return ResponseEntity.ok(ApiResponse.success(
            chatHistoryService.getAiMessages(chatId, ChatAuth.agentId(authentication)),
            "조회되었습니다."));
    }
}
