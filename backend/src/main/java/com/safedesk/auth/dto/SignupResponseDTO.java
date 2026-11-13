package com.safedesk.auth.dto;

import com.safedesk.member.entity.Member;

import java.time.LocalDateTime;

public class SignupResponseDTO {

    private final Long memberId;
    private final String loginId;
    private final String name;
    private final String role;
    private final LocalDateTime createdAt;

    private SignupResponseDTO(Long memberId, String loginId, String name, String role, LocalDateTime createdAt) {
        this.memberId = memberId;
        this.loginId = loginId;
        this.name = name;
        this.role = role;
        this.createdAt = createdAt;
    }

    public static SignupResponseDTO from(Member member) {
        return new SignupResponseDTO(
            member.getMemberId(),
            member.getLoginId(),
            member.getName(),
            member.getRole(),
            member.getCreatedAt()
        );
    }

    public Long getMemberId() { return memberId; }
    public String getLoginId() { return loginId; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
