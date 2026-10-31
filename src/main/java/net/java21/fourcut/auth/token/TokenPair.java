package net.java21.fourcut.auth.token;

/**
 * 토큰 쌍.
 *
 * @param expiresIn Access Token 유효 시간(초)
 */
public record TokenPair(String accessToken, long expiresIn, String refreshToken) {}
