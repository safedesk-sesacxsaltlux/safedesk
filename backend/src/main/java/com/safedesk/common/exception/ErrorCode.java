package com.safedesk.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력 값을 확인해주세요."),
    INVALID_PERIOD(HttpStatus.BAD_REQUEST, "조회 기간을 다시 선택해주세요."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 주소를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),

    // 회원·인증
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),

    // 제재·정책
    CUSTOMER_SANCTIONED(HttpStatus.FORBIDDEN, "상담 중 폭언으로 이용이 정지되었습니다."),
    INVALID_POLICY_VALUE(HttpStatus.BAD_REQUEST,
        "경고 발송 기준과 자동 종료 기준은 1~5회, 정지 기간은 1~365일로 입력해주세요."),

    // 상담
    CHAT_NOT_FOUND(HttpStatus.NOT_FOUND, "상담을 찾을 수 없습니다."),
    CHAT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    CHAT_ALREADY_OPEN(HttpStatus.CONFLICT, "진행 중인 상담이 존재합니다."),
    CHAT_NOT_IN_BOT(HttpStatus.CONFLICT, "상담원 연결 중에는 AI가 답하지 않습니다."),
    CHAT_ALREADY_ACTIVE(HttpStatus.CONFLICT, "이미 상담원과 연결된 상담입니다."),
    CHAT_ALREADY_ACCEPTED(HttpStatus.CONFLICT, "이미 다른 상담원이 수락한 상담입니다."),
    AGENT_BUSY(HttpStatus.CONFLICT, "진행 중인 상담을 먼저 종료해주세요."),
    AGENT_RECOVERING(HttpStatus.CONFLICT, "휴식 시간입니다. 잠시 후 수락해주세요."),
    CHAT_NOT_ACTIVE(HttpStatus.CONFLICT, "상담원과 상담 중인 상담이 아닙니다."),
    CHAT_CLOSED(HttpStatus.CONFLICT, "종료된 상담입니다.");

    private final HttpStatus status;
    private final String message;
}
