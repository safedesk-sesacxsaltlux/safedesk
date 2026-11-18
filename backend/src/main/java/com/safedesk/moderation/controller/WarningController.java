package com.safedesk.moderation.controller;

import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.common.response.ApiResponse;
import com.safedesk.moderation.dto.WarningHistoryResponseDTO;
import com.safedesk.moderation.service.WarningQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 상담별 경고 (docs/api/moderation.md MOD-05~06).
 *
 * 경로는 명세대로 /api/consultations. contract.md D-03(/api/chats 로 통일)이 확정되면 여기만 바꾼다 (#74).
 *
 * 권한 검사 위치
 *   비로그인     → 401  SecurityConfig + 여기
 *   고객         → 403  여기
 *   남의 상담    → 403  WarningQueryService
 */
@RestController
@RequestMapping("/api/consultations")
public class WarningController {

    private static final String ROLE_AGENT = "ROLE_AGENT";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final WarningQueryService warningQueryService;

    public WarningController(WarningQueryService warningQueryService) {
        this.warningQueryService = warningQueryService;
    }

    // MOD-05 상담별 경고 내역 조회
    @GetMapping("/{consultationId}/warnings")
    public ResponseEntity<ApiResponse<WarningHistoryResponseDTO>> getWarnings(
        @PathVariable long consultationId,
        Authentication authentication) {

        // 토큰이 없으면 authentication 이 null. JwtAuthenticationFilter 가 principal 에 memberId(Long)를 넣는다
        if (authentication == null || !(authentication.getPrincipal() instanceof Long memberId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        boolean isAdmin = hasRole(authentication, ROLE_ADMIN);
        boolean isAgent = hasRole(authentication, ROLE_AGENT);

        // 고객은 조회할 수 없다
        if (!isAdmin && !isAgent) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        WarningHistoryResponseDTO response = warningQueryService.getWarnings(consultationId, memberId, isAdmin);
        return ResponseEntity.ok(ApiResponse.success(response, "경고 내역 조회에 성공했습니다."));
    }

    /**
     * 로그인한 사용자가 이 역할인지. JwtAuthenticationFilter 가 "ROLE_" + role 로 권한을 하나 넣는다.
     */
    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream().anyMatch(a -> role.equals(a.getAuthority()));
    }
}
