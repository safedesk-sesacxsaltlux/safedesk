package com.safedesk.auth.jwt;

import java.util.Optional;

// 리프레시 토큰 발급·확인·폐기. (auth.md 토큰 정책, API-AUTH-06)
// 저장소를 바꿀 때(DB → Redis 등) 구현체만 바꾸면 AuthService는 그대로 쓸 수 있게 인터페이스로 둔다.
// 운영 구현: JpaRefreshTokenStore (MySQL refresh_token 테이블)
public interface RefreshTokenStore {

    // 새 리프레시 토큰을 만들어 저장하고 원문을 돌려준다. 원문은 쿠키로만 내려가고 서버에는 해시만 남는다.
    String issue(Long memberId);

    // 유효하면 memberId를 돌려주고 토큰은 지운다(한 번만 쓸 수 있음). 없거나 만료됐으면 비어 있다.
    Optional<Long> consume(String token);

    // 로그아웃 시 토큰을 지운다. null이면 아무것도 하지 않는다.
    void revoke(String token);
}
