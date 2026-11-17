package com.safedesk.history.agent;

import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.common.response.ApiResponse;
import com.safedesk.history.agent.dto.AgentCustomerHistoryResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/agents/me/customers")
public class AgentCustomerController {
    private static final String ROLE_AGENT = "ROLE_AGENT";
    private final AgentCustomerService agentCustomerService;
    // HIS-03 응대 고객별 이력. 로그인은 SecurityConfig(401), 상담원인지는 여기서 확인(403)
    // 상담원 ID 는 토큰에서 꺼낸다 (HIS-04 와 같은 이유)
    @GetMapping
    public ResponseEntity<ApiResponse<AgentCustomerHistoryResponseDTO>> search(
        Authentication authentication,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        boolean isAgent = authentication.getAuthorities().stream()
            .anyMatch((a -> ROLE_AGENT.equals(a.getAuthority())));
        if(!isAgent) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        Long agentId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(
            agentCustomerService.search(agentId, from, to, status, keyword, page, size),
            "조회되었습니다."
        ));
    }
}
