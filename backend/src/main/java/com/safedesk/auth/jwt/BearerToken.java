package com.safedesk.auth.jwt;

// 형식이 맞지 않으면 null을 돌려준다.
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
