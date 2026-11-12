package com.safedesk.auth.service;

import com.safedesk.auth.dto.CheckLoginIdResponseDTO;
import com.safedesk.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final MemberRepository memberRepository;

    public AuthService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public CheckLoginIdResponseDTO checkLoginId(String loginId) {
        boolean available = !memberRepository.existsByLoginId(loginId);
        return new CheckLoginIdResponseDTO(loginId, available);
    }
}
