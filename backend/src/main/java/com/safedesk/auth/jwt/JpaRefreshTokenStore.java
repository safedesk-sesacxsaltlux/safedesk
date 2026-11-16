package com.safedesk.auth.jwt;

import com.safedesk.auth.entity.RefreshToken;
import com.safedesk.auth.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

// 원문 대신 SHA-256 해시만 저장한다. DB가 유출돼도 해시로는 재발급을 받을 수 없다.
// TODO(인증): 이미 쓴 토큰이 다시 들어오면 그 회원의 토큰을 모두 지우는 재사용 감지
@Component
public class JpaRefreshTokenStore implements RefreshTokenStore {

    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository repository;
    private final SecureRandom random = new SecureRandom();
    private final long expirationSeconds;
    private final Clock clock;

    @Autowired
    public JpaRefreshTokenStore(RefreshTokenRepository repository,
                                @Value("${jwt.refresh-expiration-seconds}") long expirationSeconds) {
        this(repository, expirationSeconds, Clock.systemDefaultZone());
    }

    // 테스트에서 시간을 고정하기 위한 생성자
    JpaRefreshTokenStore(RefreshTokenRepository repository, long expirationSeconds, Clock clock) {
        this.repository = repository;
        this.expirationSeconds = expirationSeconds;
        this.clock = clock;
    }

    // 별도 스케줄러 없이 발급할 때 만료된 행을 정리한다.
    @Override
    @Transactional
    public String issue(Long memberId) {
        LocalDateTime now = LocalDateTime.now(clock);
        repository.deleteExpired(now);

        String token = newToken();
        repository.save(new RefreshToken(memberId, hash(token), now, now.plusSeconds(expirationSeconds)));
        return token;
    }

    @Override
    @Transactional
    public Optional<Long> consume(String token) {
        if (token == null) {
            return Optional.empty();
        }
        Optional<RefreshToken> found = repository.findByTokenHash(hash(token));
        if (found.isEmpty()) {
            return Optional.empty();
        }
        RefreshToken refreshToken = found.get();

        // 같은 토큰으로 동시에 요청하면 둘 다 조회는 되지만, 행을 실제로 지운 요청 하나만 통과한다.
        if (repository.deleteByIdAndCount(refreshToken.getRefreshTokenId()) == 0) {
            return Optional.empty();
        }
        if (refreshToken.isExpired(LocalDateTime.now(clock))) {
            return Optional.empty();
        }
        return Optional.of(refreshToken.getMemberId());
    }

    @Override
    @Transactional
    public void revoke(String token) {
        if (token != null) {
            repository.deleteByTokenHash(hash(token));
        }
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }
}
