package com.safedesk.history.agent;

import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.common.response.ApiResponse;
import com.safedesk.common.response.PageResponse;
import com.safedesk.history.agent.dto.AgentConsultationResponseDTO;
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
@RequestMapping("/api/agents/me/consultations")
public class AgentConsultationController {
    private static final String ROLE_AGENT = "ROLE_AGENT";
    private final AgentConsultationService agentConsultationService;


    // HIS-04 내가 맡은 상담 목록. 로그인은 SecurityConfig(401), 상담원인지는 여기서 확인(403)
    // "me" = 로그인한 상담원 본인 → 상담원 ID 는 파라미터로 받지 않고 토큰에서 꺼낸다 (남의 목록을 못 보게)

   @GetMapping
   public ResponseEntity<ApiResponse<PageResponse<AgentConsultationResponseDTO>>> search(
       Authentication authentication,
       @RequestParam(required = false) Long customerId,
       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
       @RequestParam(required = false) String status,
       @RequestParam(defaultValue = "0") int page,
       @RequestParam(defaultValue ="20") int size
   ) {
       boolean isAgent = authentication.getAuthorities().stream()
           .anyMatch(a -> ROLE_AGENT.equals(a.getAuthority()));
       if (!isAgent) {
           throw new BusinessException(ErrorCode.ACCESS_DENIED);
       }
       Long agentId = (Long) authentication.getPrincipal(); // JWT 필터가 principal 에 memberId 를 넣어 둔다
       return ResponseEntity.ok(ApiResponse.success(
           agentConsultationService.search(agentId, customerId,from, to, status, page, size),
           "조회되었습니다."
       ));
   }
}
