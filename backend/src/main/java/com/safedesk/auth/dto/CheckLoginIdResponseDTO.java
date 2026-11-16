package com.safedesk.auth.dto;

public class CheckLoginIdResponseDTO {

    private final String loginId;
    private final boolean available;

    public CheckLoginIdResponseDTO(String loginId, boolean available) {
        this.loginId = loginId;
        this.available = available;
    }

    public String getLoginId() {
        return loginId;
    }

    public boolean isAvailable() {
        return available;
    }
}
