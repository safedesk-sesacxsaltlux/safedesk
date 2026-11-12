package com.safedesk.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.safedesk.common.exception.ErrorCode;

// 공통 응답
public record ApiResponse<T>(
    boolean success,
    T data,
    String message,
    @JsonInclude(JsonInclude.Include.NON_NULL) String errorCode
) {

    // 성공
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, null);
    }

    //실패
    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message, T data) {
        return new ApiResponse<>(false, data, message, errorCode.name());
    }

    // 실패 응답 생성 (기본 메시지 사용)
    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return fail(errorCode, errorCode.getMessage(), null);
    }
}
