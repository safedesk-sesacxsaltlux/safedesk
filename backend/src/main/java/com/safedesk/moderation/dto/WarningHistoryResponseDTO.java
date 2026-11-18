package com.safedesk.moderation.dto;

import com.safedesk.moderation.entity.Warning;
import com.safedesk.moderation.entity.WarningType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MOD-05 상담별 경고 내역 조회 응답 (docs/api/moderation.md 「MOD-05」).
 *
 * {
 *   "consultationId": 101,
 *   "warningCount": 1,        AUTO + MANUAL 합계 (정책 4장 「수동 경고도 1회로 셈」)
 *   "autoWarningCount": 1,    AUTO 만
 *   "closeThreshold": 2,      이 상담의 정책 종료 기준
 *   "warnings": [ { "warningId": 501, "warningType": "AUTO", "createdAt": "2026-11-11T13:10:00" } ]
 * }
 *
 * ⚠️ 폭언 원문은 넣지 않는다 (NFR-005). 상담원도 이 API 를 호출한다.
 */
public class WarningHistoryResponseDTO {

    private Long consultationId;
    private long warningCount;
    private long autoWarningCount;
    private int closeThreshold;
    private List<WarningItem> warnings;

    /**
     * @param consultationId 상담 ID
     * @param warnings       이 상담의 경고 목록 (생성 시각순)
     * @param closeThreshold 이 상담의 정책 종료 기준
     */
    public static WarningHistoryResponseDTO of(Long consultationId, List<Warning> warnings, int closeThreshold) {
        WarningHistoryResponseDTO dto = new WarningHistoryResponseDTO();
        dto.consultationId = consultationId;
        dto.closeThreshold = closeThreshold;
        dto.warningCount = warnings.size();
        dto.autoWarningCount = warnings.stream().filter(w -> w.getWarningType() == WarningType.AUTO).count();
        // 경고가 없으면 null 이 아니라 빈 목록 [] (프론트가 바로 반복문을 돌릴 수 있다)
        dto.warnings = warnings.stream().map(WarningItem::from).toList();

        return dto;
    }

    public Long getConsultationId() {
        return consultationId;
    }

    public long getWarningCount() {
        return warningCount;
    }

    public long getAutoWarningCount() {
        return autoWarningCount;
    }

    public int getCloseThreshold() {
        return closeThreshold;
    }

    public List<WarningItem> getWarnings() {
        return warnings;
    }

    /** warnings 배열의 한 건 */
    public static class WarningItem {

        private Long warningId;
        private WarningType warningType;
        private LocalDateTime createdAt;

        /**
         * Warning 엔티티에서 응답에 필요한 3개만 꺼낸다.
         * abuseLogId·issuedBy 는 넣지 않는다 (명세에 없음).
         */
        public static WarningItem from(Warning warning) {
            WarningItem item = new WarningItem();
            item.warningId = warning.getWarningId();
            item.warningType = warning.getWarningType();
            item.createdAt = warning.getCreatedAt();
            return item;
        }

        public Long getWarningId() {
            return warningId;
        }

        public WarningType getWarningType() {
            return warningType;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
    }
}
