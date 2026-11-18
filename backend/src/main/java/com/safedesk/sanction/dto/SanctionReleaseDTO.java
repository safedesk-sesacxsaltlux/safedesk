package com.safedesk.sanction.dto;

import java.time.LocalDateTime;

public record SanctionReleaseDTO(LocalDateTime releasedAt, PolicyAdminDTO releasedBy, String reason) {
}
