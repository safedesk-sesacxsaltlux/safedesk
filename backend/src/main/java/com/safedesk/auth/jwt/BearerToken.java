package com.safedesk.auth.jwt;

// "Authorization: Bearer {token}" 헤더에서 토큰만 꺼낸다. 형식이 맞지 않으면 null.
public final class BearerToken {

    private static final String PREFIX = "Bearer ";

    private BearerToken() {
    }

    public static String resolve(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(PREFIX)) {
            return null;
        }
        String token = authorizationHeader.substring(PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
