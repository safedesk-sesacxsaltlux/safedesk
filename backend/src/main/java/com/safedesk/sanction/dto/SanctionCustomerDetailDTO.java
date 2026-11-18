package com.safedesk.sanction.dto;

import com.safedesk.member.entity.Member;

import java.time.LocalDateTime;

public record SanctionCustomerDetailDTO(Long memberId, String loginId, String name, LocalDateTime joinedAt) {

    public static SanctionCustomerDetailDTO from(Member member) {
        return new SanctionCustomerDetailDTO(
            member.getMemberId(), member.getLoginId(), member.getName(), member.getCreatedAt());
    }
}
