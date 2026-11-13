package com.safedesk.auth.controller;

import com.safedesk.auth.dto.AuthApiResponse;
import com.safedesk.auth.dto.CheckLoginIdResponseDTO;
import com.safedesk.auth.dto.SignupRequestDTO;
import com.safedesk.auth.dto.SignupResponseDTO;
import com.safedesk.auth.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthApiResponse<SignupResponseDTO> signup(@Valid @RequestBody SignupRequestDTO request) {
        SignupResponseDTO result = authService.signup(request);
        return AuthApiResponse.success(result, "회원가입이 완료되었습니다.");
    }
}
