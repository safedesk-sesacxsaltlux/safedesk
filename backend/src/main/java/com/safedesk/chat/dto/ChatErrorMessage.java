package com.safedesk.chat.dto;

import com.safedesk.common.exception.ErrorCode;

/**
 * WebSocket 오류 알림. 보낸 사람에게만 /user/queue/errors 로 간다 (contract.md WebSocket 「오류」).
 *
 * { "type": "ERROR", "errorCode": "CHAT_CLOSED", "message": "종료된 상담입니다." }
 */
public class ChatErrorMessage {

    private final String type = "ERROR";
    private String errorCode;
    private String message;

    public static ChatErrorMessage of(ErrorCode errorCode, String message) {
        ChatErrorMessage dto = new ChatErrorMessage();
        dto.errorCode = errorCode.name();
        dto.message = message;
        return dto;
    }

    public String getType() {
        return type;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }
}
