package com.safedesk.moderation.dto;

/**
 * WebSocket /topic/chats/{consultationId} 로 발행하는 경고 알림 (contract.md WebSocket 「WARNING_ISSUED」).
 *
 * {
 *   "type": "WARNING_ISSUED",
 *   "consultationId": 101,
 *   "warningId": 501,
 *   "warningCount": 1,
 *   "message": "부적절한 표현이 감지되었습니다. 한 번 더 폭언 시 상담이 종료되고 이용이 제한될 수 있습니다."
 * }
 *
 * ⚠️ 원문은 절대 넣지 않는다 (NFR-005). 같은 토픽을 상담원도 구독한다.
 */
public class WarningIssuedMessage {

    private final String type = "WARNING_ISSUED";
    private Long consultationId;
    private Long warningId;
    private long warningCount;
    private String message;

    public static WarningIssuedMessage from(WarningResult result) {
        WarningIssuedMessage dto = new WarningIssuedMessage();
        dto.consultationId = result.consultationId();
        dto.warningId = result.warningId();
        dto.warningCount = result.warningCount();
        dto.message = buildMessage(result.closeThreshold() - result.warningCount());

        return dto;
    }

    /**
     * 남은 횟수에 맞춰 문구를 만든다.
     * 기본 정책(종료 기준 2회)이면 첫 경고에서 남은 횟수가 1 → "한 번 더 폭언 시 …" (REQ-016, 정책 4장 문구 그대로)
     * 관리자가 기준을 3회로 바꾸면 첫 경고는 "2번 더 폭언 시 …" 가 된다 (체크리스트 S3)
     */
    private static String buildMessage(long remaining) {
        String times = (remaining == 1) ? "한 번" : remaining + "번";
        return "부적절한 표현이 감지되었습니다. " + times + " 더 폭언 시 상담이 종료되고 이용이 제한될 수 있습니다.";
    }

    public String getType() {
        return type;
    }

    public Long getConsultationId() {
        return consultationId;
    }

    public Long getWarningId() {
        return warningId;
    }

    public long getWarningCount() {
        return warningCount;
    }

    public String getMessage() {
        return message;
    }
}
