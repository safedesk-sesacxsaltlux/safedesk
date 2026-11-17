package com.safedesk.history.agent;
import com.safedesk.chat.entity.ConsultationStatus;
import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.common.response.PageResponse;
import com.safedesk.history.agent.dto.AgentCustomerHistoryResponseDTO;
import com.safedesk.history.agent.dto.AgentCustomerHistoryResponseDTO.CustomerRow;
import com.safedesk.history.agent.dto.AgentCustomerHistoryResponseDTO.LatestConsultation;
import com.safedesk.history.agent.dto.AgentCustomerHistoryResponseDTO.Stats;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// HIS-03 응대 고객별 이력 (docs/api/history.md)
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AgentCustomerService {
    private static final int MAX_SIZE = 50;
    private static final int MAX_PERIOD_DAYS = 366;
    // 명세: 기간 366일 초과면 INVALID_PERIOD
    // 상담번호 검색: "C-000123", "c123", "123" 모두 허용 → 숫자만 꺼낸다 (HIS-05 와 같은 규칙)
    private static final Pattern CONSULTATION_NO = Pattern.compile("^[Cc]-?(\\d{1,18})$|^(\\d{1,18})$");
    private final AgentCustomerRepository repository;
    // agentId 는 로그인한 상담원 본인 (컨트롤러가 토큰에서 꺼내 넘김)
    public AgentCustomerHistoryResponseDTO search(Long agentId, LocalDate from, LocalDate to, String status,
                                                  String keyword, int page, int size)
    {
        // 1) 입력 검증
        if (from != null && to != null
            && (from.isAfter(to) || ChronoUnit.DAYS.between(from, to) + 1 > MAX_PERIOD_DAYS)) {
            throw new BusinessException(ErrorCode.INVALID_PERIOD);
        }
        if (page < 0 || size < 1 || size > MAX_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        // 2) 화면 값 → 쿼리 조건
        String statusFilter = toStatusFilter(status);
        LocalDateTime fromAt = from == null ? null : from.atStartOfDay();
        LocalDateTime toAt = to == null ? null : to.plusDays(1).atStartOfDay();
        String trimmed = keyword == null || keyword.isBlank() ? null : keyword.trim();
        Long keywordId = trimmed == null ? null : parseConsultationNo(trimmed);
        String keywordLike = trimmed == null ? null : "%" + trimmed + "%";
        // 3) 카드(기간만) + 목록(기간·상태·키워드) 조회 → 화면용 변환
       Stats stats = toStats(repository.stats(agentId, fromAt, toAt));
       PageResponse<CustomerRow> customers = PageResponse.from(
           repository.searchCustomers(agentId, fromAt, toAt, statusFilter, keywordId, keywordLike,
               PageRequest.of(page, size))
               .map(this::toCustomerRow));
        return new AgentCustomerHistoryResponseDTO(stats, customers);
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
    private Long parseConsultationNo(String keyword) {
        Matcher m = CONSULTATION_NO.matcher(keyword);
        if (!m.matches()) {
            return null;
        }
        return Long.parseLong((m.group(1) != null ? m.group(1) : m.group(2)));
    }

    // 집계 결과는 상담이 없으면 null 이 올 수 있어서 0 으로 바꾼다. 평균은 초 단위 반올림
    private Stats toStats(AgentCustomerStatsRow row) {
        return new Stats(
            toLong(row.getServedCustomerCount()),
            toLong(row.getTotalConsultationCount()),
            row.getAvgDurationSeconds() == null ? 0 : Math.round(row.getAvgDurationSeconds().doubleValue()),
            toLong(row.getAbuseTerminatedCount())
        );
    }
    private CustomerRow toCustomerRow(AgentCustomerRow row) {
        return new CustomerRow(
            row.getCustomerId(),
            row.getCustomerLoginId(),
            toLong(row.getHasViolationHistory()) > 0,
            toLong(row.getMyConsultationCount()),
            new LatestConsultation(
                row.getConsultationId(),
                String.format("C-%06d", row.getConsultationId()),
                row.getStartedAt(),
                row.getInquirySummary(),
                toLong(row.getAbuseCount()),
                row.getStatus()
            )
        );
    }
    private static long toLong (Number n) {
        return n == null ? 0 : n.longValue();
    }


}
