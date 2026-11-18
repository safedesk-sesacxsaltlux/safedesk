package com.safedesk.sanction.dto;

public record PolicyChangeDTO(
    String field,
    String fieldLabel,
    String beforeValue,
    String afterValue
) {
}
