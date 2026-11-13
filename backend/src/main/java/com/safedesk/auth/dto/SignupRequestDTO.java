package com.safedesk.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class SignupRequestDTO {

    @NotBlank
    @Pattern(regexp = "^[a-z0-9]{4,20}$")
    private String loginId;

    @NotBlank
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,20}$")
    private String password;

    @NotBlank
    private String passwordConfirm;

    @NotBlank
    @Size(max = 20)
    private String name;

    private boolean policyAgreed;

    protected SignupRequestDTO() {
    }

    public String getLoginId() { return loginId; }
    public String getPassword() { return password; }
    public String getPasswordConfirm() { return passwordConfirm; }
    public String getName() { return name; }
    public boolean isPolicyAgreed() { return policyAgreed; }
}

