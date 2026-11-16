package com.safedesk.auth.jwt;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 로그아웃한 토큰을 만료 시각까지 기억한다. (auth.md API-AUTH-04)
// 만료 시각이 지나면 토큰 자체가 검증에서 거부되므로 그 뒤로는 기억할 필요가 없다.
//
// TODO(인증): 지금은 서버 메모리에 저장한다.
//  - 서버를 재시작하면 목록이 사라져서, 로그아웃한 토큰이 남은 만료 시간(최대 jwt.expiration-seconds) 동안 다시 쓸 수 있다.
//  - 서버를 여러 대 띄우면 서버끼리 목록을 공유하지 못한다.
//  운영 수준이 필요하면 Redis(만료 시간 TTL) 또는 DB 테이블로 옮긴다.
@Component
public class TokenBlacklist {

    private final Map<String, Instant> tokens = new ConcurrentHashMap<>();
    private final Clock clock;

    public TokenBlacklist() {
        this(Clock.systemUTC());
    }

    // 테스트에서 시간을 고정하기 위한 생성자
    TokenBlacklist(Clock clock) {
        this.clock = clock;
    }

    public void add(String token, Instant expiresAt) {
        removeExpired();
        tokens.put(token, expiresAt);
    }

    public boolean contains(String token) {
        Instant expiresAt = tokens.get(token);
        if (expiresAt == null) {
            return false;
        }
        if (!expiresAt.isAfter(clock.instant())) {
            tokens.remove(token);
            return false;
        }
        return true;
    }

    // 별도 스케줄러 없이, 새로 추가할 때 만료된 항목을 정리해서 메모리가 계속 늘지 않게 한다.
    private void removeExpired() {
        Instant now = clock.instant();
        tokens.values().removeIf(expiresAt -> !expiresAt.isAfter(now));
    }

    int size() {
        return tokens.size();
    }
}
