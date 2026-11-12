package com.safedesk.auth.dto;

public class AuthApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;

    private AuthApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> AuthApiResponse<T> success(T data, String message) {
        return new AuthApiResponse<>(true, message, data);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
