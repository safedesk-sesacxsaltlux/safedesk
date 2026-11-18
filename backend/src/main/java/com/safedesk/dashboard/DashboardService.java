package com.safedesk.dashboard;

import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.dashboard.DashboardRows.CardRow;
import com.safedesk.dashboard.DashboardRows.CloseTypeRow;
import com.safedesk.dashboard.DashboardRows.DailyCountRow;
import com.safedesk.dashboard.DashboardRows.RecentSanctionRow;
import com.safedesk.dashboard.dto.DashboardResponseDTO;
import com.safedesk.dashboard.dto.DashboardResponseDTO.Cards;
import com.safedesk.dashboard.dto.DashboardResponseDTO.CloseType;
import com.safedesk.dashboard.dto.DashboardResponseDTO.CloseTypes;
import com.safedesk.dashboard.dto.DashboardResponseDTO.DailyTrend;
import com.safedesk.dashboard.dto.DashboardResponseDTO.Period;
import com.safedesk.dashboard.dto.DashboardResponseDTO.RecentSanction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// HIS-07 운영 대시보드 (docs/api/history.md)
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final DashboardRepository repository;

    public DashboardResponseDTO getDashboard(String period) {
        return getDashboard(period, LocalDate.now(), LocalDateTime.now());
    }

    // 테스트에서 "오늘"과 "지금"을 고정하려고 따로 둔 메서드
    DashboardResponseDTO getDashboard(String period, LocalDate today, LocalDateTime now) {
        // 1) 기간: 오늘 포함 1일 / 7일 / 30일
        String type = period == null || period.isBlank() ? "TODAY" : period;
        int days = switch (type) {
            case "TODAY" -> 1;
            case "7D" -> 7;
            case "30D" -> 30;
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT);   // 명세 INVALID_PARAMETER → D-06 으로 통일
        };
        LocalDate fromDate = today.minusDays(days - 1);
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();   // 오늘 하루 끝까지

        // 2) 집계
        CloseTypeRow close = repository.closeTypes(from, to);
        long totalClosed = toLong(close.getTotalClosedCount());
        long aiClosed = toLong(close.getAiClosedCount());

        return new DashboardResponseDTO(
            new Period(type, fromDate, today),
            toCards(repository.cards(from, to, now), aiClosed, totalClosed),
            toDailyTrend(fromDate, days, repository.dailyConsultations(from, to), repository.dailyAbuse(from, to)),
            new CloseTypes(
                totalClosed,
                closeType(aiClosed, totalClosed),
                closeType(toLong(close.getAgentClosedCount()), totalClosed),
                closeType(toLong(close.getQueueCanceledCount()), totalClosed),
                closeType(toLong(close.getTerminatedCount()), totalClosed)
            ),
            repository.recentSanctions().stream().map(r -> toRecentSanction(r, now)).toList()
        );
    }

    private Cards toCards(CardRow row, long aiClosed, long totalClosed) {
        return new Cards(
            toLong(row.getTotalConsultationCount()),
            rate(aiClosed, totalClosed),
            toLong(row.getAbuseDetectedCount()),
            toLong(row.getAutoTerminatedCount()),
            toLong(row.getNewSanctionCount()),
            toLong(row.getCurrentSuspendedCustomerCount())
        );
    }

    // 쿼리는 건수가 있는 날만 돌려주므로, 기간의 모든 날짜를 만들고 없는 날은 0 으로 채운다
    private List<DailyTrend> toDailyTrend(LocalDate fromDate, int days,
                                          List<DailyCountRow> consultations, List<DailyCountRow> abuses) {
        Map<String, Long> consultationByDay = toMap(consultations);
        Map<String, Long> abuseByDay = toMap(abuses);

        List<DailyTrend> result = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = fromDate.plusDays(i);
            String key = date.toString();   // "2026-11-06" (쿼리의 date_format 과 같은 모양)
            result.add(new DailyTrend(date,
                consultationByDay.getOrDefault(key, 0L),
                abuseByDay.getOrDefault(key, 0L)));
        }
        return result;
    }

    private Map<String, Long> toMap(List<DailyCountRow> rows) {
        return rows.stream().collect(Collectors.toMap(DailyCountRow::getDay, r -> toLong(r.getCnt())));
    }

    private CloseType closeType(long count, long total) {
        return new CloseType(count, rate(count, total));
    }

    // 소수점 한 자리 비율. 분모가 0 이면 null (명세)
    private Double rate(long count, long total) {
        if (total == 0) {
            return null;
        }
        return Math.round(count * 1000.0 / total) / 10.0;
    }

    // 정지 상태는 컬럼이 아니라 계산한다 (contract.md SanctionStatus)
    private RecentSanction toRecentSanction(RecentSanctionRow row, LocalDateTime now) {
        String status;
        if (row.getReleasedAt() != null) {
            status = "RELEASED";
        } else if (!row.getEndAt().isAfter(now)) {
            status = "EXPIRED";
        } else {
            status = "SUSPENDED";
        }
        return new RecentSanction(
            row.getSanctionId(),
            row.getCustomerId(),
            row.getCustomerLoginId(),
            toLong(row.getViolationCount()),
            (int) toLong(row.getSuspendDays()),
            row.getEndAt(),
            status
        );
    }

    private static long toLong(Number n) {
        return n == null ? 0 : n.longValue();
    }
}
