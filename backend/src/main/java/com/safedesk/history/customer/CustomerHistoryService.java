package com.safedesk.history.customer;

import com.safedesk.chat.entity.CloseReason;
import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.common.response.PageResponse;
import com.safedesk.history.customer.dto.CustomerConsultationResponseDTO;
import com.safedesk.history.customer.dto.CustomerConsultationResponseDTO.Summary;
import com.safedesk.history.customer.dto.CustomerSanctionResponseDTO;
import com.safedesk.history.customer.dto.CustomerSanctionResponseDTO.CurrentSanction;
import com.safedesk.history.customer.dto.CustomerSanctionResponseDTO.PolicyGuide;
import com.safedesk.sanction.entity.SanctionPolicy;
import com.safedesk.sanction.repository.SanctionPolicyRepository;
import com.safedesk.sanction.repository.ViolationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

// HIS-01 내 상담 이력, HIS-02 내 이용 상태 (docs/api/history.md)
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerHistoryService {

    private static final int MAX_SIZE = 50;
    private static final long SECONDS_PER_DAY = 24 * 60 * 60;

    private final CustomerHistoryRepository repository;
    private final ViolationRepository violationRepository;
    private final SanctionPolicyRepository sanctionPolicyRepository;

    // ---------- HIS-01 ----------

    public PageResponse<CustomerConsultationResponseDTO> getMyConsultations(Long customerId, String type,
                                                                            int page, int size) {
        if (page < 0 || size < 1 || size > MAX_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return PageResponse.from(
                repository.searchMyConsultations(customerId, toAgentHandled(type), PageRequest.of(page, size))
                        .map(this::toConsultation));
    }

    // AI → false(상담원 없음), AGENT → true(상담원 있음), ALL·빈 값 → null(조건 없음)
    private Boolean toAgentHandled(String type) {
        if (type == null || type.isBlank() || "ALL".equals(type)) {
            return null;
        }
        return switch (type) {
            case "AI" -> false;
            case "AGENT" -> true;
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT);
        };
    }

    private CustomerConsultationResponseDTO toConsultation(CustomerConsultationRow row) {
        return new CustomerConsultationResponseDTO(
                row.consultationId(),
                String.format("C-%06d", row.consultationId()),
                row.startedAt(),
                row.closedAt(),
                row.agentId() == null ? "AI" : "AGENT",
                row.status(),
                row.inquirySummary(),
                new Summary(row.inquiryCategory(), row.processSummary(), row.result()),
                row.violationCount() != null && row.violationCount() > 0
        );
    }

    // ---------- HIS-02 ----------

    public CustomerSanctionResponseDTO getMySanction(Long customerId) {
        return getMySanction(customerId, LocalDateTime.now());
    }

    // 테스트에서 "지금"을 고정하려고 따로 둔 메서드
    CustomerSanctionResponseDTO getMySanction(Long customerId, LocalDateTime now) {
        CurrentSanction current = repository.findActiveSanction(customerId, now)
                .map(s -> toCurrentSanction(s, now))
                .orElse(null);

        return new CustomerSanctionResponseDTO(
                current == null ? "NONE" : "SUSPENDED",
                current == null,
                violationRepository.countByCustomerId(customerId),
                current,
                policyGuide()
        );
    }

    private CurrentSanction toCurrentSanction(ActiveSanctionInfo s, LocalDateTime now) {
        return new CurrentSanction(
                s.getSanctionId(),
                s.getSuspendDays().intValue(),
                s.getStartAt(),
                s.getEndAt(),
                remainingDays(now, s.getEndAt()),
                toReason(s.getCloseReason())
        );
    }

    // 해제까지 남은 시간을 일 단위로 올림 (1초라도 남으면 1일). 화면의 D-N
    private long remainingDays(LocalDateTime now, LocalDateTime endAt) {
        long seconds = Duration.between(now, endAt).getSeconds();
        return (seconds + SECONDS_PER_DAY - 1) / SECONDS_PER_DAY;
    }

    // 사유 문구는 상담 시작(CUS-01) 이용정지 안내와 같다
    private String toReason(String closeReason) {
        return CloseReason.ABUSE_MANUAL.equals(closeReason)
                ? "상담 중 폭언 (상담원 종료)"
                : "상담 중 반복 폭언 (자동 종료)";
    }

    // 가장 최근 정책 버전의 정지 일수 (1회, 2회, 3회 이상)
    private List<PolicyGuide> policyGuide() {
        return sanctionPolicyRepository.findFirstByOrderByPolicyIdDesc()
                .map(this::toGuide)
                .orElse(List.of());
    }

    private List<PolicyGuide> toGuide(SanctionPolicy p) {
        return List.of(
                new PolicyGuide(1, p.getFirstDays(), false),
                new PolicyGuide(2, p.getSecondDays(), false),
                new PolicyGuide(3, p.getThirdDays(), true)
        );
    }
}
