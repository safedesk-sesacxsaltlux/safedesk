package com.safedesk.chat.dto;

import java.time.LocalDateTime;

public record CancelResponseDTO(Long chatId, String status, String closeReason, LocalDateTime closedAt) { }
