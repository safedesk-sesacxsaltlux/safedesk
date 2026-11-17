package com.safedesk.sanction.service;

import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.common.response.PageResponse;
import com.safedesk.member.repository.MemberRepository;
import com.safedesk.sanction.dto.*;
import com.safedesk.sanction.entity.SanctionPolicy;
import com.safedesk.sanction.repository.SanctionPolicyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SanctionPolicyService {

    private static final long DEFAULT_POLICY_ID = 1L;
    private static final int MAX_PAGE_SIZE = 50;

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

    @Transactional
    public PolicyVersionDTO updatePolicy(PolicyUpdateRequestDTO request, Long adminId) {
        int warn = request.warningThreshold();
        int close = request.autoCloseThreshold();
        int first = request.suspensionDays().first();
        int second = request.suspensionDays().second();
        int third = request.suspensionDays().thirdOrMore();

        if (!inRange(warn, 1, 5) || !inRange(close, 1, 5) || close <= warn
            || !inRange(first, 1, 365) || !inRange(second, 1, 365) || !inRange(third, 1, 365)) {
            throw new BusinessException(ErrorCode.INVALID_POLICY_VALUE);
        }

        SanctionPolicy current = sanctionPolicyRepository.findFirstByOrderByPolicyIdDesc()
            .orElseThrow(() -> new IllegalStateException("제재 정책이 없습니다."));

        if (current.getWarnThreshold() == warn
            && current.getCloseThreshold() == close
            && current.getFirstDays() == first
            && current.getSecondDays() == second
            && current.getThirdDays() == third) {
            return PolicyVersionDTO.of(current, findAdmin(current.getCreatedBy()));
        }

        SanctionPolicy saved = sanctionPolicyRepository.save(
            SanctionPolicy.create(warn, close, first, second, third, adminId));

        return PolicyVersionDTO.of(saved, findAdmin(saved.getCreatedBy()));
    }

    @Transactional(readOnly = true)
    public PageResponse<PolicyHistoryDTO> getHistories(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        Page<SanctionPolicy> policies = sanctionPolicyRepository.findAllByOrderByPolicyIdDesc(PageRequest.of(page, size));
        List<SanctionPolicy> rows = policies.getContent();

        List<PolicyHistoryDTO> histories = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            SanctionPolicy policy = rows.get(i);
            SanctionPolicy previous = (i + 1 < rows.size())
                ? rows.get(i + 1)
                : sanctionPolicyRepository.findFirstByPolicyIdLessThanOrderByPolicyIdDesc(policy.getPolicyId())
                    .orElse(null);
            histories.add(PolicyHistoryDTO.of(policy, previous, findAdmin(policy.getCreatedBy())));
        }

        return PageResponse.from(new PageImpl<>(histories, policies.getPageable(), policies.getTotalElements()));
    }

    private PolicyAdminDTO findAdmin(Long memberId) {
        if (memberId == null) {
            return null;
        }
        return memberRepository.findById(memberId)
            .map(PolicyAdminDTO::from)
            .orElse(null);
    }

    private boolean inRange(int value, int min, int max) {
        return value >= min && value <= max;
    }
}
