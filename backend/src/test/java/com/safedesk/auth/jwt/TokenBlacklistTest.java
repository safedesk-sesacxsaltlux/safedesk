package com.safedesk.auth.jwt;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

public class TokenBlacklistTest {

    private static final Instant NOW = Instant.parse("2026-11-16T10:00:00Z");

    private final TokenBlacklist blacklist = new TokenBlacklist(Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void 추가한_토큰은_만료_전까지_포함된다() {
        blacklist.add("token-a", NOW.plusSeconds(60));

        assertThat(blacklist.contains("token-a")).isTrue();
        assertThat(blacklist.contains("token-b")).isFalse();
    }

    @Test
    void 만료_시각이_지난_토큰은_포함되지_않고_목록에서_지운다() {
        blacklist.add("token-a", NOW.minusSeconds(1));

        assertThat(blacklist.contains("token-a")).isFalse();
        assertThat(blacklist.size()).isZero();
    }

    @Test
    void 새로_추가할_때_만료된_항목을_정리한다() {
        blacklist.add("expired-1", NOW.minusSeconds(10));
        blacklist.add("expired-2", NOW.minusSeconds(5));
        blacklist.add("valid", NOW.plusSeconds(60));

        assertThat(blacklist.size()).isEqualTo(1);
        assertThat(blacklist.contains("valid")).isTrue();
    }
}
