package com.safedesk.auth.dto;

import com.safedesk.member.entity.Member;

// 로그인 응답의 member, 내 정보 조회(/api/auth/me) 응답에서 함께 쓰는 회원 정보
public class MemberInfoDTO {

    private final Long memberId;
    private final String loginId;
    private final String name;
    private final String role;

    private MemberInfoDTO(Long memberId, String loginId, String name, String role) {
        this.memberId = memberId;
        this.loginId = loginId;
        this.name = name;
        this.role = role;
    }

    public static MemberInfoDTO from(Member member) {
        return new MemberInfoDTO(
            member.getMemberId(),
            member.getLoginId(),
            member.getName(),
            member.getRole()
        );
    }

    public Long getMemberId() { return memberId; }
    public String getLoginId() { return loginId; }
    public String getName() { return name; }
    public String getRole() { return role; }
}
