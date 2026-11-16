package com.safedesk.chat.controller;

import com.safedesk.chat.dto.CancelResponseDTO;
import com.safedesk.chat.dto.HandoffResponseDTO;
import com.safedesk.chat.service.CustomerChatService;
import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 고객 AI 상담 API (CUS-01~04). 상담원 쪽 API 는 ChatController.
 */
@RestController
@RequestMapping("/api/chats")
public class CustomerChatController {

    private final CustomerChatService customerChatService;

    public CustomerChatController(CustomerChatService customerChatService) {
        this.customerChatService = customerChatService;
    }

    // CUS-03 상담원 연결 요청
    @PatchMapping("/{chatId}/handoff")
    public ResponseEntity<ApiResponse<HandoffResponseDTO>> handoff(
        @PathVariable long chatId,
        @AuthenticationPrincipal Long customerId) {

        HandoffResponseDTO response = customerChatService.handoff(chatId, requireLogin(customerId));
        return ResponseEntity.ok(ApiResponse.success(response, "상담원 연결을 요청했습니다."));
    }

    // CUS-04 고객 상담 종료 (/close 는 상담원 종료 API-006 이 써서 /cancel 로 구분)
    @PatchMapping("/{chatId}/cancel")
    public ResponseEntity<ApiResponse<CancelResponseDTO>> cancel(
        @PathVariable long chatId,
        @AuthenticationPrincipal Long customerId) {

        CancelResponseDTO response = customerChatService.cancel(chatId, requireLogin(customerId));
        return ResponseEntity.ok(ApiResponse.success(response, "상담이 종료되었습니다."));
    }

    // SecurityConfig 가 아직 모든 경로를 허용(permitAll)해서, 토큰 없이 오면 null 이 들어온다.
    // 경로별 권한 규칙이 켜지면 이 확인은 필요 없어진다.
    private long requireLogin(Long memberId) {
        if (memberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return memberId;
    }
}
