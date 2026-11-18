package com.safedesk.history.customer;

import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.common.response.ApiResponse;
import com.safedesk.common.response.PageResponse;
import com.safedesk.history.customer.dto.CustomerConsultationResponseDTO;
import com.safedesk.history.customer.dto.CustomerSanctionResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customers/me")
public class CustomerHistoryController {

    private static final String ROLE_CUSTOMER = "ROLE_CUSTOMER";

    private final CustomerHistoryService customerHistoryService;

    // HIS-01 내 상담 이력 목록. type: ALL(기본) | AI | AGENT
    @GetMapping("/consultations")
    public ResponseEntity<ApiResponse<PageResponse<CustomerConsultationResponseDTO>>> getMyConsultations(
            Authentication authentication,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long customerId = customerId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                customerHistoryService.getMyConsultations(customerId, type, page, size),
                "조회되었습니다."));
    }

    // HIS-02 내 이용 상태(제재). 정지 중인 고객도 조회할 수 있어야 한다 (이용정지 안내 화면)
    @GetMapping("/sanction")
    public ResponseEntity<ApiResponse<CustomerSanctionResponseDTO>> getMySanction(Authentication authentication) {
        Long customerId = customerId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                customerHistoryService.getMySanction(customerId),
                "조회되었습니다."));
    }

    // 고객만 쓸 수 있다(403). 고객 ID 는 파라미터가 아니라 토큰에서 꺼낸다 (남의 이력을 못 보게)
    private Long customerId(Authentication authentication) {
        boolean isCustomer = authentication.getAuthorities().stream()
                .anyMatch(a -> ROLE_CUSTOMER.equals(a.getAuthority()));
        if (!isCustomer) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return (Long) authentication.getPrincipal();
    }
}
