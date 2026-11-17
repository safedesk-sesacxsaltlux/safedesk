package com.safedesk.sanction.service;

import com.safedesk.member.repository.MemberRepository;
import com.safedesk.sanction.dto.PolicyAdminDTO;
import com.safedesk.sanction.dto.PolicyDefaultsDTO;
import com.safedesk.sanction.dto.PolicyResponseDTO;
import com.safedesk.sanction.dto.PolicyVersionDTO;
import com.safedesk.sanction.entity.SanctionPolicy;
import com.safedesk.sanction.repository.SanctionPolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SanctionPolicyService {

    private static final long DEFAULT_POLICY_ID = 1L;

    private final SanctionPolicyRepository sanctionPolicyRepository;
    private final MemberRepository memberRepository;

    public SanctionPolicyService(SanctionPolicyRepository sanctionPolicyRepository,
                                 MemberRepository memberRepository) {
        this.sanctionPolicyRepository = sanctionPolicyRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public PolicyResponseDTO getPolicy() {
        SanctionPolicy current = sanctionPolicyRepository.findFirstByOrderByPolicyIdDesc()
            .orElseThrow(() -> new IllegalStateException("제재 정책이 없습니다."));
        SanctionPolicy defaults = sanctionPolicyRepository.findById(DEFAULT_POLICY_ID)
            .orElseThrow(() -> new IllegalStateException("기본 제재 정책이 없습니다."));

        return new PolicyResponseDTO(
            PolicyVersionDTO.of(current, findAdmin(current.getCreatedBy())),
            PolicyDefaultsDTO.from(defaults)
        );
    }

    private PolicyAdminDTO findAdmin(Long memberId) {
        if (memberId == null) {
            return null;
        }
        return memberRepository.findById(memberId)
            .map(PolicyAdminDTO::from)
            .orElse(null);
    }
}
