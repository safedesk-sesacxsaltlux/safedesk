package com.safedesk.auth.jwt;

import com.safedesk.auth.entity.RefreshToken;
import com.safedesk.auth.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// DB 없이 리포지토리를 Mockito로 대신해서, 저장소가 어떤 값을 저장하고 어떤 조건에서 통과시키는지 확인한다.
public class JpaRefreshTokenStoreTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final Instant NOW = Instant.parse("2026-11-16T01:00:00Z");
    private static final LocalDateTime NOW_LOCAL = LocalDateTime.ofInstant(NOW, ZONE);

    private final RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
    private final JpaRefreshTokenStore store =
        new JpaRefreshTokenStore(repository, 604800, Clock.fixed(NOW, ZONE));

    @Test
    void 발급하면_원문이_아니라_해시와_만료시각을_저장하고_만료된_행을_정리한다() {
        String token = store.issue(13L);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        verify(repository).deleteExpired(NOW_LOCAL);

        assertThat(saved.getValue().getMemberId()).isEqualTo(13L);
        assertThat(saved.getValue().getTokenHash()).isNotEqualTo(token).hasSize(64);
        assertThat(saved.getValue().getTokenHash()).isEqualTo(JpaRefreshTokenStore.hash(token));
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(NOW_LOCAL.plusSeconds(604800));
    }

    @Test
    void 매번_다른_토큰을_발급한다() {
        assertThat(store.issue(13L)).isNotEqualTo(store.issue(13L));
    }

    @Test
    void 유효한_토큰을_쓰면_행을_지우고_memberId를_돌려준다() {
        RefreshToken row = row(7L, "token-a", NOW_LOCAL.plusDays(1));
        when(repository.findByTokenHash(JpaRefreshTokenStore.hash("token-a"))).thenReturn(Optional.of(row));
        when(repository.deleteByIdAndCount(7L)).thenReturn(1);

        assertThat(store.consume("token-a")).contains(13L);
        verify(repository).deleteByIdAndCount(7L);
    }

    @Test
    void 동시에_다른_요청이_먼저_지웠으면_통과하지_않는다() {
        RefreshToken row = row(7L, "token-a", NOW_LOCAL.plusDays(1));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(row));
        when(repository.deleteByIdAndCount(7L)).thenReturn(0);

        assertThat(store.consume("token-a")).isEmpty();
    }

    @Test
    void 만료된_토큰은_행을_지우고_통과하지_않는다() {
        RefreshToken row = row(7L, "token-a", NOW_LOCAL);
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(row));
        when(repository.deleteByIdAndCount(7L)).thenReturn(1);

        assertThat(store.consume("token-a")).isEmpty();
        verify(repository).deleteByIdAndCount(7L);
    }

    @Test
    void 없는_토큰과_null은_통과하지_않는다() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThat(store.consume("unknown")).isEmpty();
        assertThat(store.consume(null)).isEmpty();
        verify(repository, never()).deleteByIdAndCount(any());
    }

    @Test
    void 로그아웃하면_해시로_행을_지운다() {
        store.revoke("token-a");
        store.revoke(null);

        verify(repository).deleteByTokenHash(JpaRefreshTokenStore.hash("token-a"));
    }

    private RefreshToken row(Long id, String token, LocalDateTime expiresAt) {
        RefreshToken row = new RefreshToken(13L, JpaRefreshTokenStore.hash(token), NOW_LOCAL, expiresAt);
        ReflectionTestUtils.setField(row, "refreshTokenId", id);
        return row;
    }
}
