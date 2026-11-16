package com.safedesk.auth.repository;

import com.safedesk.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // 지운 행 수를 돌려준다. 같은 토큰으로 동시에 요청하면 먼저 지운 쪽만 1, 나머지는 0이 된다.
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.refreshTokenId = :id")
    int deleteByIdAndCount(@Param("id") Long id);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.tokenHash = :tokenHash")
    int deleteByTokenHash(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt <= :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
