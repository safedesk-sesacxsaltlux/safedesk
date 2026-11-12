package com.safedesk.common.exception;

import lombok.Getter;

@Getter
// 오류가 발생했을 때 오류 코드, 메시지, 추가 데이터를 담을 수 있는 예외 클래스
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Object data; // 실패해도 화면에 넘겨야 하는 값


    // 오류 코드만 전달받는 생성자
    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage(), null);
    }

    // 오류 코드 + 메시지를 받음
    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    // 실제 값 저장
    public BusinessException(ErrorCode errorCode, String message, Object data) {
        super(message);
        this.errorCode = errorCode;
        this.data = data;
    }
}
