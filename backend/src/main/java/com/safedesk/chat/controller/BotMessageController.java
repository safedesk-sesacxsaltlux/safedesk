package com.safedesk.chat.controller;

import com.safedesk.chat.dto.BotMessageRequestDTO;
import com.safedesk.chat.dto.BotMessageResponseDTO;
import com.safedesk.chat.service.BotMessageService;
import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CUS-02 AI 상담 메시지 전송·응답.
 * CustomerChatController 와 같은 /api/chats 아래지만, 동시에 작업 중인 CUS-01 과 충돌하지 않도록 파일을 나눴다.
 */
@RestController
@RequestMapping("/api/chats")
public class BotMessageController {

    private static final String ROLE_CUSTOMER = "ROLE_CUSTOMER";

    private final BotMessageService botMessageService;

    public BotMessageController(BotMessageService botMessageService) {
        this.botMessageService = botMessageService;
    }

    @PostMapping("/{chatId}/bot-messages")
    public ResponseEntity<ApiResponse<BotMessageResponseDTO>> send(
        @PathVariable long chatId,
        @Valid @RequestBody BotMessageRequestDTO request,
        Authentication authentication) {

        long customerId = requireCustomer(authentication);
        BotMessageResponseDTO response = botMessageService.send(chatId, customerId, request.content());
        return ResponseEntity.ok(ApiResponse.success(response, "응답이 생성되었습니다."));
    }

    // SecurityConfig 가 아직 모든 경로를 허용해서, 로그인·고객 여부를 여기서 확인한다
    private long requireCustomer(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long memberId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        boolean isCustomer = authentication.getAuthorities().stream()
            .anyMatch(authority -> ROLE_CUSTOMER.equals(authority.getAuthority()));
        if (!isCustomer) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return memberId;
    }
}
