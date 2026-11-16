package com.safedesk.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

// 기기마다 한 행이라 회원 한 명이 여러 행을 가질 수 있다.
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refresh_token_id")
    private Long refreshTokenId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected RefreshToken() {
    }

    public RefreshToken(Long memberId, String tokenHash, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.memberId = memberId;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public Long getRefreshTokenId() { return refreshTokenId; }
    public Long getMemberId() { return memberId; }
    public String getTokenHash() { return tokenHash; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
