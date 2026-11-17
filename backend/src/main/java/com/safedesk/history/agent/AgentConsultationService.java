package com.safedesk.history.agent;

import com.safedesk.chat.entity.ConsultationStatus;
import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.common.response.PageResponse;
import com.safedesk.history.agent.dto.AgentConsultationResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AgentConsultationService {
    private static final int MAX_SIZE = 50;
    private final AgentConsultationRepository repository;
    // agentId 는 화면이 보내는 값이 아니라 로그인한 상담원 본인 (컨트롤러가 토큰에서 꺼내 넘김)
    public PageResponse<AgentConsultationResponseDTO> search(Long agentId, Long customerId, LocalDate from, LocalDate to, String status,
                                                             int page, int size)
    {
        // 1) 입력 검증
        if(from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(ErrorCode.INVALID_PERIOD);
        }
        if (page < 0 || size < 1 || size > MAX_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        // 2) 화면 값 → 쿼리 조건
        String statusFilter = toStatusFilter(status);
        LocalDateTime fromAt = from == null ? null : from.atStartOfDay();
        LocalDateTime toAt = to == null ? null : to.plusDays(1).atStartOfDay();
        // to 날짜 하루 포함
        // 3) 조회 → 화면용 변환
        return PageResponse.from(
            repository.search(agentId, customerId, statusFilter, fromAt, toAt, PageRequest.of(page,size))
                .map(this::toResponse)
        );
    }
    private String toStatusFilter(String status) {
        if(status == null || status.isBlank() || "ALL".equals(status)) {
            return null;
        }
        if (ConsultationStatus.CLOSED.equals(status) || ConsultationStatus.TERMINATED.equals(status)) {
            return status;
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT);
    }
    private AgentConsultationResponseDTO toResponse(AgentConsultationRow row) {
        return new AgentConsultationResponseDTO(
            row.consultationId(),
            String.format("C-%06d", row.consultationId()),
            row.customerLoginId(),
            row.startedAt(),
            row.closedAt(),
            row.inquirySummary(),
            row.abuseCount() == null ? 0 : row.abuseCount(),
            row.status()
        );
    }
}
