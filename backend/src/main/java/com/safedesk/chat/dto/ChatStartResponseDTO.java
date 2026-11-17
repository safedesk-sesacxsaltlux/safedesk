package com.safedesk.chat.dto;

import java.time.LocalDateTime;

public record ChatStartResponseDTO(Long chatId, String status, LocalDateTime createdAt) {

}
