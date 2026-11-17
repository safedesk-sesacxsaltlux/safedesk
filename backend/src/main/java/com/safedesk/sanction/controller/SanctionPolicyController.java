package com.safedesk.sanction.controller;

import com.safedesk.common.response.ApiResponse;
import com.safedesk.sanction.dto.PolicyResponseDTO;
import com.safedesk.sanction.service.SanctionPolicyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
