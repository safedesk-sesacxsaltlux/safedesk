package com.safedesk.sanction.controller;

import com.safedesk.common.response.ApiResponse;
import com.safedesk.sanction.dto.ViolationOriginalDTO;
import com.safedesk.sanction.service.SanctionAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/violations")
public class ViolationAdminController {

    private final SanctionAdminService sanctionAdminService;

    public ViolationAdminController(SanctionAdminService sanctionAdminService) {
        this.sanctionAdminService = sanctionAdminService;
    }

    // API-ADM-07 위반 원문 조회
    @GetMapping("/{violationId}/original")
    public ResponseEntity<ApiResponse<ViolationOriginalDTO>> getOriginal(
        @PathVariable long violationId,
        @AuthenticationPrincipal Long adminId) {

        return ResponseEntity.ok(ApiResponse.success(
            sanctionAdminService.getOriginal(violationId, adminId), "요청이 처리되었습니다."));
    }
}
