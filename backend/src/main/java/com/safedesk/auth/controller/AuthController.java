package com.safedesk.auth.controller;

import com.safedesk.auth.dto.AuthApiResponse;
import com.safedesk.auth.dto.CheckLoginIdResponseDTO;
import com.safedesk.auth.service.AuthService;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/check-login-id")
    public AuthApiResponse<CheckLoginIdResponseDTO> checkLoginId(
            @RequestParam @Pattern(regexp = "^[a-z0-9]{4,20}$") String loginId) {

        CheckLoginIdResponseDTO result = authService.checkLoginId(loginId);
        String message = result.isAvailable() ? "사용 가능한 아이디입니다." : "이미 사용 중인 아이디입니다.";

        return AuthApiResponse.success(result, message);
    }
}
