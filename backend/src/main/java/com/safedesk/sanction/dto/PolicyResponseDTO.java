package com.safedesk.sanction.dto;

public record PolicyResponseDTO(
    PolicyVersionDTO current,
    PolicyDefaultsDTO defaults
) {
}
