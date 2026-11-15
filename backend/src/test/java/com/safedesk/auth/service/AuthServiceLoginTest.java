package com.safedesk.auth.service;

import com.safedesk.auth.dto.LoginRequestDTO;
import com.safedesk.auth.dto.LoginResponseDTO;
import com.safedesk.auth.jwt.JwtClaims;
import com.safedesk.auth.jwt.JwtProvider;
import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.member.entity.Member;
import com.safedesk.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthServiceLoginTest {

    private static final String RAW_PASSWORD = "pass1234!";

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtProvider jwtProvider = new JwtProvider("test-secret-key-must-be-at-least-32-bytes!!", 3600);
    private final AuthService authService = new AuthService(memberRepository, passwordEncoder, jwtProvider);

    private Member active;
    private Member inactive;

    @BeforeEach
    void setUp() {
        active = member(1L, "user01", "ACTIVE");
        inactive = member(2L, "user02", "INACTIVE");
        when(memberRepository.findByLoginId("user01")).thenReturn(Optional.of(active));
        when(memberRepository.findByLoginId("user02")).thenReturn(Optional.of(inactive));
        when(memberRepository.findByLoginId("nobody")).thenReturn(Optional.empty());
    }

    @Test
    void 성공하면_토큰과_회원정보를_돌려준다() {
        LoginResponseDTO result = authService.login(request("user01", RAW_PASSWORD));

        assertThat(result.getTokenType()).isEqualTo("Bearer");
        assertThat(result.getExpiresIn()).isEqualTo(3600);
        assertThat(result.getMember().getMemberId()).isEqualTo(1L);
        assertThat(result.getMember().getRole()).isEqualTo("CUSTOMER");

        JwtClaims claims = jwtProvider.validate(result.getAccessToken());
        assertThat(claims.memberId()).isEqualTo(1L);
        assertThat(claims.role()).isEqualTo("CUSTOMER");
    }

    @Test
    void 없는_아이디는_LOGIN_FAILED() {
        assertError(request("nobody", RAW_PASSWORD), ErrorCode.LOGIN_FAILED);
    }

    @Test
    void 비밀번호가_틀리면_LOGIN_FAILED() {
        assertError(request("user01", "wrong1234!"), ErrorCode.LOGIN_FAILED);
    }

    @Test
    void INACTIVE_회원은_비밀번호가_맞으면_MEMBER_INACTIVE() {
        assertError(request("user02", RAW_PASSWORD), ErrorCode.MEMBER_INACTIVE);
    }

    @Test
    void INACTIVE_회원이라도_비밀번호가_틀리면_LOGIN_FAILED() {
        // 비밀번호를 모르는 사람에게 계정 상태를 알려주지 않는다
        assertError(request("user02", "wrong1234!"), ErrorCode.LOGIN_FAILED);
    }

    private Member member(Long memberId, String loginId, String status) {
        Member member = Member.createCustomer(loginId, passwordEncoder.encode(RAW_PASSWORD), "홍길동");
        ReflectionTestUtils.setField(member, "memberId", memberId);
        ReflectionTestUtils.setField(member, "status", status);
        return member;
    }

    // LoginRequestDTO의 기본 생성자는 protected라서 다른 패키지인 테스트에서는 BeanUtils로 만든다
    private LoginRequestDTO request(String loginId, String password) {
        LoginRequestDTO request = BeanUtils.instantiateClass(LoginRequestDTO.class);
        ReflectionTestUtils.setField(request, "loginId", loginId);
        ReflectionTestUtils.setField(request, "password", password);
        return request;
    }

    private void assertError(LoginRequestDTO request, ErrorCode expected) {
        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(expected);
    }
}
