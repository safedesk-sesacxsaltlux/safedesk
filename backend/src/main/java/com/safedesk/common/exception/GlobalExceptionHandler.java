package com.safedesk.common.exception;

import com.safedesk.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import org.springframework.security.access.AccessDeniedException;

@Slf4j
@RestControllerAdvice
// 예외를 공통 처리하고, 필요한 로그를 남기며, 사용자에게 일관된 오류 응답을 보내기 위한 클래스
public class GlobalExceptionHandler {
    // 1. 우리가 직접 던진 예외
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        log.warn("비즈니스 예외: code={}, message={}", code.name(), e.getMessage());
        return ResponseEntity.status(code.getStatus())
            .body(ApiResponse.fail(code, e.getMessage(), e.getData()));
    }

    // 2. @Valid @RequestBody 검증 실패 (예: @NotBlank 위반)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException e) {
        e.getBindingResult().getFieldErrors().forEach(error ->
            log.warn("입력값 검증 실패: field={}, reason={}", error.getField(), error.getDefaultMessage()));
        return toResponse(ErrorCode.INVALID_INPUT);
    }

    // 3. 그 밖의 잘못된 요청 (깨진 JSON, 타입 불일치, 필수 파라미터 누락, @RequestParam 검증 실패)
    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class,
        HandlerMethodValidationException.class
    })
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(Exception e) {
        log.warn("잘못된 요청: {}", e.getClass().getSimpleName());
        return toResponse(ErrorCode.INVALID_INPUT);
    }

    // 4. Controller 안에서 권한 검사(@PreAuthorize)에 걸린 경우
    // 접근 권한이 없는 경우 처리
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException e) {
        return toResponse(ErrorCode.ACCESS_DENIED);
    }

    // 5. 없는 주소, 지원하지 않는 Method
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(NoResourceFoundException e) {
        return toResponse(ErrorCode.NOT_FOUND);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return toResponse(ErrorCode.METHOD_NOT_ALLOWED);
    }

    // 6. 위에서 못 잡은 모든 예외 = 버그
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception e) {
        log.error("처리하지 않은 예외", e); // 스택 트레이스는 서버 로그에만
        return toResponse(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ApiResponse<Object>> toResponse(ErrorCode code) {
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.fail(code));
    }

}
