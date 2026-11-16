package com.safedesk.auth.service;

import com.safedesk.auth.dto.CheckLoginIdResponseDTO;
import com.safedesk.auth.dto.LoginRequestDTO;
import com.safedesk.auth.dto.LoginResponseDTO;
import com.safedesk.auth.dto.MemberInfoDTO;
import com.safedesk.auth.dto.SignupRequestDTO;
import com.safedesk.auth.dto.SignupResponseDTO;
import com.safedesk.auth.jwt.JwtClaims;
import com.safedesk.auth.jwt.JwtProvider;
import com.safedesk.auth.jwt.TokenBlacklist;
import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.member.entity.Member;
import com.safedesk.member.repository.MemberRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final TokenBlacklist tokenBlacklist;

    public AuthService(MemberRepository memberRepository, PasswordEncoder passwordEncoder,
                       JwtProvider jwtProvider, TokenBlacklist tokenBlacklist) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.tokenBlacklist = tokenBlacklist;
    }

    @Transactional(readOnly = true)
    public CheckLoginIdResponseDTO checkLoginId(String loginId) {
        boolean available = !memberRepository.existsByLoginId(loginId);
        return new CheckLoginIdResponseDTO(loginId, available);
    }

    @Transactional
    public SignupResponseDTO signup(SignupRequestDTO request) {

        if (!request.isPolicyAgreed()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "상담원 보호 정책에 동의해주세요.");
        }

        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "비밀번호가 일치하지 않습니다.");
        }

        if (memberRepository.existsByLoginId(request.getLoginId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        Member member = Member.createCustomer(request.getLoginId(), encodedPassword, request.getName().trim());
        // 중복 확인과 저장 사이에 같은 아이디로 동시 가입하면 DB 유니크 제약(uk_member_login_id)에 걸린다.
        // save()는 커밋 시점에 INSERT하므로 여기서 잡으려면 saveAndFlush()로 즉시 실행해야 한다.
        try {
            Member saved = memberRepository.saveAndFlush(member);
            return SignupResponseDTO.from(saved);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
    }

    // 확인 순서: 회원 조회 → 비밀번호 → 상태.
    // 아이디가 없을 때와 비밀번호가 틀릴 때 같은 응답을 주고, 상태는 비밀번호가 맞을 때만 알려준다.
    // (비밀번호를 모르는 사람이 아이디 존재 여부나 계정 상태를 알아낼 수 없게 하기 위함)
    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginRequestDTO request) {
        Member member = memberRepository.findByLoginId(request.getLoginId())
            .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        if (!"ACTIVE".equals(member.getStatus())) {
            throw new BusinessException(ErrorCode.MEMBER_INACTIVE);
        }

        String accessToken = jwtProvider.createToken(member.getMemberId(), member.getRole());
        return LoginResponseDTO.of(accessToken, jwtProvider.getExpirationSeconds(), member);
    }

    // 토큰은 유효하지만 그 사이 회원이 삭제된 경우도 로그인이 필요한 상태로 본다.
    @Transactional(readOnly = true)
    public MemberInfoDTO getMe(Long memberId) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        return MemberInfoDTO.from(member);
    }

    // 토큰을 만료 시각까지 블랙리스트에 올린다. 상담 중이어도 상담은 종료하지 않는다. (auth.md API-AUTH-04)
    public void logout(String token) {
        JwtClaims claims = jwtProvider.validate(token);
        tokenBlacklist.add(token, claims.expiresAt());
    }
}
