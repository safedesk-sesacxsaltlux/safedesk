package com.safedesk.sanction.dto;

import com.safedesk.sanction.repository.ViolationMessageRow;

import java.time.LocalDateTime;

public record ViolationMessageDTO(
    Long messageId,
    LocalDateTime sentAt,
    String classification,
    String detectedBy,
    Long warningOrder,
    String originalContent,
    String maskedContent
) {

    private static final String ABUSIVE = "ABUSIVE";

    public static ViolationMessageDTO from(ViolationMessageRow row) {
        return new ViolationMessageDTO(
            row.getMessageId(),
            row.getSentAt(),
            ABUSIVE,
            row.getDetectedBy(),
            row.getWarningOrder(),
            row.getOriginalContent(),
            row.getMaskedContent()
        );
    }
}
