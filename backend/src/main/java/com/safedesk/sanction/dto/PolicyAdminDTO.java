package com.safedesk.sanction.dto;

import com.safedesk.member.entity.Member;

public record PolicyAdminDTO(
    Long memberId,
    String name
) {

    public static PolicyAdminDTO from(Member member) {
        return new PolicyAdminDTO(member.getMemberId(), member.getName());
    }
}
