package com.safedesk.auth.dto;

import com.safedesk.member.entity.Member;

public class LoginResponseDTO {

    private static final String TOKEN_TYPE = "Bearer";

    private final String accessToken;
    private final String tokenType;
    private final long expiresIn; // 초 단위
    private final MemberInfoDTO member;

    private LoginResponseDTO(String accessToken, long expiresIn, MemberInfoDTO member) {
        this.accessToken = accessToken;
        this.tokenType = TOKEN_TYPE;
        this.expiresIn = expiresIn;
        this.member = member;
    }

    public static LoginResponseDTO of(String accessToken, long expiresIn, Member member) {
        return new LoginResponseDTO(accessToken, expiresIn, MemberInfoDTO.from(member));
    }

    public String getAccessToken() { return accessToken; }
    public String getTokenType() { return tokenType; }
    public long getExpiresIn() { return expiresIn; }
    public MemberInfoDTO getMember() { return member; }
}
