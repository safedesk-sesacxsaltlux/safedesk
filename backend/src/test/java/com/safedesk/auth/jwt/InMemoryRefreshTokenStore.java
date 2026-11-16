package com.safedesk.auth.jwt;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// 테스트용 가짜 저장소. DB 없이 AuthService의 발급·재발급·로그아웃 흐름을 확인할 때 쓴다.
// 운영에서는 JpaRefreshTokenStore를 쓴다.
public class InMemoryRefreshTokenStore implements RefreshTokenStore {

    private final Map<String, Long> tokens = new ConcurrentHashMap<>();

    @Override
    public String issue(Long memberId) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, memberId);
        return token;
    }

    @Override
    public Optional<Long> consume(String token) {
        return token == null ? Optional.empty() : Optional.ofNullable(tokens.remove(token));
    }

    @Override
    public void revoke(String token) {
        if (token != null) {
            tokens.remove(token);
        }
    }
}
