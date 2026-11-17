package com.safedesk.history.admin;

import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.common.response.ApiResponse;
import com.safedesk.common.response.PageResponse;
import com.safedesk.history.admin.dto.AdminConsultationResponseDTO;
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
@RequestMapping("/api/admin/consultations")
public class AdminConsultationController {
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private final AdminConsultationService adminConsultationService;

    // HIS-05 전체 상담 이력
    // required = false: 안 보내도 되는 검색 조건 / defaultValue: 안 보내면 쓸 기본값
    // @DateTimeFormat(ISO.DATE): "2026-11-03" 형식을 LocalDate 로 바꿔 준다 (형식이 틀리면 400 INVALID_INPUT)
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminConsultationResponseDTO>>> search(
        Authentication authentication,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String handledBy,
        @RequestParam(required = false) Long agentId,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size)
    {
        // 관리자가 아니면 403 (공통 응답 형식으로 내려가도록 여기서 확인)
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(a -> ROLE_ADMIN.equals(a.getAuthority()));
        if(!isAdmin) {
            throw new BusinessException((ErrorCode.ACCESS_DENIED));
        }
        return ResponseEntity.ok(ApiResponse.success(
            adminConsultationService.search(from, to, status, handledBy,agentId, keyword, page, size),
            "조회되었습니다."
        ));
    }

}
