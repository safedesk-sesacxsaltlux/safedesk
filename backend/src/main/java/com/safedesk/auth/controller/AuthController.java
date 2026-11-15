package com.safedesk.auth.controller;

import com.safedesk.auth.dto.CheckLoginIdResponseDTO;
import com.safedesk.auth.dto.LoginRequestDTO;
import com.safedesk.auth.dto.LoginResponseDTO;
import com.safedesk.auth.dto.MemberInfoDTO;
import com.safedesk.auth.dto.SignupRequestDTO;
import com.safedesk.auth.dto.SignupResponseDTO;
import com.safedesk.auth.service.AuthService;
import com.safedesk.common.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/check-login-id")
    public ResponseEntity<ApiResponse<CheckLoginIdResponseDTO>> checkLoginId(
        @RequestParam @Pattern(regexp = "^[a-z0-9]{4,20}$") String loginId) {

        CheckLoginIdResponseDTO result = authService.checkLoginId(loginId);
        String message = result.isAvailable() ? "사용 가능한 아이디입니다." : "이미 사용 중인 아이디입니다.";

        return ResponseEntity.ok(ApiResponse.success(result, message));
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponseDTO>> signup(@Valid @RequestBody SignupRequestDTO request) {
        SignupResponseDTO result = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(result, "회원가입이 완료되었습니다."));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(@Valid @RequestBody LoginRequestDTO request) {
        LoginResponseDTO result = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(result, "로그인되었습니다."));
    }

    // SecurityConfig에서 인증이 필요한 경로로 지정되어 있어서, 여기까지 오면 memberId는 항상 있다.
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberInfoDTO>> me(@AuthenticationPrincipal Long memberId) {
        MemberInfoDTO result = authService.getMe(memberId);
        return ResponseEntity.ok(ApiResponse.success(result, "요청이 처리되었습니다."));
    }
}
