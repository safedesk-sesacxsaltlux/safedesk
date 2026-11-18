package com.safedesk.chat.controller;

import com.safedesk.chat.dto.AgentRecoveryResponseDTO;
import com.safedesk.chat.service.AgentRecoveryService;
import com.safedesk.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AgentRecoveryController {

    private final AgentRecoveryService agentRecoveryService;

    public AgentRecoveryController(AgentRecoveryService agentRecoveryService) {
        this.agentRecoveryService = agentRecoveryService;
    }

    // API-008 상담원 회복 시간 조회 (REQ-033)
    @GetMapping("/api/agents/me/recovery")
    public ResponseEntity<ApiResponse<AgentRecoveryResponseDTO>> getRecovery(Authentication authentication) {
        Long agentId = ChatAuth.agentId(authentication);
        return ResponseEntity.ok(ApiResponse.success(agentRecoveryService.getRecovery(agentId), "조회되었습니다."));
    }
}
