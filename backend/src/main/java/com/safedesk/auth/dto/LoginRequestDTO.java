package com.safedesk.auth.dto;

import jakarta.validation.constraints.NotBlank;

// 로그인은 저장된 값과 일치하는지만 확인하므로 형식 검사 없이 필수 값만 검사한다.
public class LoginRequestDTO {

    @NotBlank
    private String loginId;

    @NotBlank
    private String password;

    protected LoginRequestDTO() {
    }

    public String getLoginId() { return loginId; }
    public String getPassword() { return password; }
}
