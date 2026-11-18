package com.safedesk.sanction.controller;

import com.safedesk.common.response.ApiResponse;
import com.safedesk.common.response.PageResponse;
import com.safedesk.sanction.dto.SanctionDetailDTO;
import com.safedesk.sanction.dto.SanctionListItemDTO;
import com.safedesk.sanction.dto.SanctionReleaseRequestDTO;
import com.safedesk.sanction.dto.SanctionReleaseResponseDTO;
import com.safedesk.sanction.service.SanctionAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 관리자 제재 관리 (API-ADM-04~06). ADMIN 권한은 SecurityConfig 의 /api/admin/** 규칙이 확인한다
@RestController
@RequestMapping("/api/admin/sanctions")
public class SanctionAdminController {

    private final SanctionAdminService sanctionAdminService;

    public SanctionAdminController(SanctionAdminService sanctionAdminService) {
        this.sanctionAdminService = sanctionAdminService;
    }

    // API-ADM-04 제재 목록
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SanctionListItemDTO>>> getSanctions(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Integer durationDays,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {

        PageResponse<SanctionListItemDTO> result =
            sanctionAdminService.getSanctions(keyword, status, durationDays, page, size);
        return ResponseEntity.ok(ApiResponse.success(result, "요청이 처리되었습니다."));
    }

    // API-ADM-05 제재 상세
    @GetMapping("/{sanctionId}")
    public ResponseEntity<ApiResponse<SanctionDetailDTO>> getSanction(@PathVariable long sanctionId) {
        return ResponseEntity.ok(ApiResponse.success(
            sanctionAdminService.getSanction(sanctionId), "요청이 처리되었습니다."));
    }

    // API-ADM-06 제재 수동 해제
    @PostMapping("/{sanctionId}/release")
    public ResponseEntity<ApiResponse<SanctionReleaseResponseDTO>> release(
        @PathVariable long sanctionId,
        @RequestBody SanctionReleaseRequestDTO request,
        @AuthenticationPrincipal Long adminId) {

        return ResponseEntity.ok(ApiResponse.success(
            sanctionAdminService.release(sanctionId, adminId, request.reason()), "제재가 해제되었습니다."));
    }
}
