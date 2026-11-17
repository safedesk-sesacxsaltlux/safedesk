package com.safedesk.history.admin;

import com.safedesk.chat.entity.ConsultationStatus;
import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.common.response.PageResponse;
import com.safedesk.history.admin.dto.AdminConsultationResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminConsultationService {
    private static final int MAX_SIZE = 50;
    // 상담번호 검색: "C-000123", "c123", "123" 모두 허용 → 숫자만 꺼낸다
    private static final Pattern CONSULTATION_NO = Pattern.compile("^[Cc]-?(\\d{1,18})$|^(\\d{1,18})$");
    private final AdminConsultationRepository repository;
    public PageResponse<AdminConsultationResponseDTO> search(LocalDate from, LocalDate to, String status,
                                                             String handledBy, Long agentId, String keyword,
                                                             int page, int size)
    {
        // 1. 입력검증
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(ErrorCode.INVALID_PERIOD);
        }
        if(page < 0 || size < 1 || size > MAX_SIZE ) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        // 2) 화면 값 → 쿼리 조건 (ALL 이나 빈 값은 null = 조건 없음)
        String statusFilter = toStatusFilter(status);
        Boolean agentHandled = toAgentHandled(handledBy);
        LocalDateTime fromAt = from == null ? null : from.atStartOfDay();
        // to 는 "그날 끝까지" 포함 → 다음 날 0시 "미만"으로 비교 (23:59:59.999 같은 경계 문제가 없다)
        LocalDateTime toAt = to == null ? null : to.plusDays(1).atStartOfDay();
        // 키워드는 상담번호로도, 고객 아이디(부분 일치)로도 찾는다
        String trimmed = keyword == null || keyword.isBlank() ? null : keyword.trim();
        Long keywordId = trimmed == null ? null : parseConsultationNo(trimmed);
        String keywordLike = trimmed == null ? null : "%" + trimmed + "%";
        // 3) 조회 → 화면용으로 변환. Page.map 은 페이지 정보(전체 건수 등)는 유지하고 내용만 바꾼다
        return PageResponse.from(
            repository.search(statusFilter, fromAt, toAt, agentHandled, agentId, keywordId, keywordLike,
                PageRequest.of(page, size))
                .map(this::toResponse)
        );
    }
    private String toStatusFilter(String status) {
        if(status == null || status.isBlank() || "ALL". equals(status)) {
            return null;
        }
        if(ConsultationStatus.CLOSED.equals(status) || ConsultationStatus.TERMINATED.equals(status)) {
            return status;
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT);
    }
    // AI → false(상담원 없음), AGENT → true(상담원 있음), ALL → null(조건 없음)
    private Boolean toAgentHandled(String handledBy) {
        if(handledBy == null || handledBy.isBlank() || "ALL".equals(handledBy)) {
            return null;
        }
        return switch(handledBy) {
            case "AI" -> false;
            case "AGENT" -> true;
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT);
        };
    }
    // "C-000123" → 123. 상담번호 모양이 아니면 null (그때는 고객 아이디로만 찾는다)
    private Long parseConsultationNo(String keyword) {
        Matcher m = CONSULTATION_NO.matcher(keyword);
        if (!m.matches()) {
            return null;
        }
        return Long.parseLong(m.group(1) != null ? m.group(1) : m.group(2));
    }
    private AdminConsultationResponseDTO toResponse(AdminConsultationRow row) {
        return new AdminConsultationResponseDTO(
            row.consultationId(),
            String.format("C-%06d", row.consultationId()),
            row.startedAt(),
            row.customerId(),
            row.customerLoginId(),
            row.agentId(),
            row.agentName(),
            row.agentId() == null ? "AI" : "AGENT", // 상담원이 없으면 ai 단독
            row.abuseCount() == null ? 0 : row.abuseCount(),
            row.status()
        );
    }

}
