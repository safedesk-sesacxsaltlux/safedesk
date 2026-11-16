package com.safedesk.auth.controller;

import com.safedesk.auth.dto.CheckLoginIdResponseDTO;
import com.safedesk.auth.dto.LoginRequestDTO;
import com.safedesk.auth.dto.LoginResponseDTO;
import com.safedesk.auth.dto.LoginResult;
import com.safedesk.auth.dto.MemberInfoDTO;
import com.safedesk.auth.dto.SignupRequestDTO;
import com.safedesk.auth.dto.SignupResponseDTO;
import com.safedesk.auth.jwt.BearerToken;
import com.safedesk.auth.jwt.RefreshTokenCookie;
import com.safedesk.auth.service.AuthService;
import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.common.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookie refreshTokenCookie;

    public AuthController(AuthService authService, RefreshTokenCookie refreshTokenCookie) {
        this.authService = authService;
        this.refreshTokenCookie = refreshTokenCookie;
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
        LoginResult result = authService.login(request);
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.create(result.refreshToken()))
            .body(ApiResponse.success(result.response(), "로그인되었습니다."));
    }

    // 실패해도 쿠키를 지워야 해서 GlobalExceptionHandler에 넘기지 않고 직접 응답한다.
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> refresh(
        @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken) {

        try {
            LoginResult result = authService.refresh(refreshToken);
            return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.create(result.refreshToken()))
                .body(ApiResponse.success(result.response(), "토큰이 재발급되었습니다."));
        } catch (BusinessException e) {
            ErrorCode code = e.getErrorCode();
            return ResponseEntity.status(code.getStatus())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.expire())
                .body(ApiResponse.fail(code, e.getMessage(), null));
        }
    }

    // 인증 필요 경로(SecurityConfig)라서 memberId는 null이 아니다.
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberInfoDTO>> me(@AuthenticationPrincipal Long memberId) {
        MemberInfoDTO result = authService.getMe(memberId);
        return ResponseEntity.ok(ApiResponse.success(result, "요청이 처리되었습니다."));
    }

    // HttpOnly 쿠키는 프론트가 지울 수 없어서 서버가 지운다.
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
        @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken) {

        authService.logout(BearerToken.resolve(authorization), refreshToken);
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.expire())
            .body(ApiResponse.success(null, "로그아웃되었습니다."));
    }
}
