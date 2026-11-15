package com.safedesk.auth.service;

import com.safedesk.auth.dto.CheckLoginIdResponseDTO;
import com.safedesk.auth.dto.SignupRequestDTO;
import com.safedesk.auth.dto.SignupResponseDTO;
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

    public AuthService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
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
}
