package com.safedesk.chat.dto;

import java.time.LocalDateTime;

public record HandoffResponseDTO (Long chatId, String status, LocalDateTime queuedAt) {

}
