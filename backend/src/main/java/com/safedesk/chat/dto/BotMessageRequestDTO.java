package com.safedesk.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// CUS-02 요청. 빈 메시지·500자 초과는 GlobalExceptionHandler 가 400 INVALID_INPUT 으로 바꾼다
public record BotMessageRequestDTO(
    @NotBlank @Size(max = 500) String content
) {
}
