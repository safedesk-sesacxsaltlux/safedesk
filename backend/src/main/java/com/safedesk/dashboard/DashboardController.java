package com.safedesk.dashboard;

import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.common.response.ApiResponse;
import com.safedesk.dashboard.dto.DashboardResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dashboard")
public class DashboardController {
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private final DashboardService dashboardService;
    // HIS-07 운영 대시보드. 로그인은 SecurityConfig(401), 관리자인지는 여기서 확인(403)
    // period: TODAY(기본) | 7D | 30D
    @GetMapping
    public ResponseEntity<ApiResponse<DashboardResponseDTO>> getDashboard(
        Authentication authentication,
        @RequestParam(required = false) String period
    ) {
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch((a -> ROLE_ADMIN.equals(a.getAuthority())));
        if(!isAdmin) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getDashboard(period), "조회되었습니다."));
    }
}
