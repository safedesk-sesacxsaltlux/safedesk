package com.safedesk.auth.service;

import com.safedesk.auth.dto.CheckLoginIdResponseDTO;
import com.safedesk.auth.dto.SignupRequestDTO;
import com.safedesk.auth.dto.SignupResponseDTO;
import com.safedesk.member.entity.Member;
import com.safedesk.member.repository.MemberRepository;
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
            throw new IllegalArgumentException("상담원 보호 정책에 동의해주세요.");
        }

        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        if (memberRepository.existsByLoginId(request.getLoginId())) {
            throw new IllegalStateException("이미 사용 중인 아이디입니다.");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        Member member = Member.createCustomer(request.getLoginId(), encodedPassword, request.getName().trim());
        Member saved = memberRepository.save(member);
        return SignupResponseDTO.from(saved);
    }
}
