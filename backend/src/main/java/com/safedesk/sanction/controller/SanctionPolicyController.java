package com.safedesk.sanction.controller;

import com.safedesk.common.response.ApiResponse;
import com.safedesk.common.response.PageResponse;
import com.safedesk.sanction.dto.PolicyHistoryDTO;
import com.safedesk.sanction.dto.PolicyResponseDTO;
import com.safedesk.sanction.dto.PolicyUpdateRequestDTO;
import com.safedesk.sanction.dto.PolicyVersionDTO;
import com.safedesk.sanction.service.SanctionPolicyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/policies")
public class SanctionPolicyController {

    private final SanctionPolicyService sanctionPolicyService;

    public SanctionPolicyController(SanctionPolicyService sanctionPolicyService) {
        this.sanctionPolicyService = sanctionPolicyService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PolicyResponseDTO>> getPolicy() {
        PolicyResponseDTO result = sanctionPolicyService.getPolicy();
        return ResponseEntity.ok(ApiResponse.success(result, "요청이 처리되었습니다."));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<PolicyVersionDTO>> updatePolicy(
        @Valid @RequestBody PolicyUpdateRequestDTO request,
        @AuthenticationPrincipal Long memberId) {

        PolicyVersionDTO result = sanctionPolicyService.updatePolicy(request, memberId);
        return ResponseEntity.ok(ApiResponse.success(result, "정책이 저장되었습니다."));
    }

    @GetMapping("/histories")
    public ResponseEntity<ApiResponse<PageResponse<PolicyHistoryDTO>>> getHistories(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {

        PageResponse<PolicyHistoryDTO> result = sanctionPolicyService.getHistories(page, size);
        return ResponseEntity.ok(ApiResponse.success(result, "요청이 처리되었습니다."));
    }
}
