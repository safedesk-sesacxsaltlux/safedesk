package com.safedesk.history.detail;

import com.safedesk.common.response.ApiResponse;
import com.safedesk.history.detail.dto.ConsultationDetailResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/consultations")
public class ConsultationDetailController {

    private static final String ROLE_PREFIX = "ROLE_";

    private final ConsultationDetailService detailService;

    // HIS-06 상담 상세. SecurityConfig 에서 authenticated() 로 걸어서 여기 오면 항상 로그인 상태
    @GetMapping("/{consultationId}/history")
    public ResponseEntity<ApiResponse<ConsultationDetailResponseDTO>> getDetail(
            @PathVariable long consultationId,
            Authentication authentication) {

        // JWT 필터가 principal 에 memberId(Long)를 넣어 둔다
        Long memberId = (Long) authentication.getPrincipal();
        String role = roleOf(authentication);

        return ResponseEntity.ok(ApiResponse.success(
                detailService.getDetail(consultationId, memberId, role),
                "조회되었습니다."));
    }

    // 권한은 "ROLE_CUSTOMER" 형태로 들어 있다 → ROLE_ 떼고 "CUSTOMER" 로 넘김. 없으면 null → 서비스에서 403
    private String roleOf(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith(ROLE_PREFIX))
                .map(a -> a.substring(ROLE_PREFIX.length()))
                .findFirst()
                .orElse(null);
    }
}
