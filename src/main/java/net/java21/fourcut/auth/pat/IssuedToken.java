package net.java21.fourcut.auth.pat;

/**
 * 새로 발급한 토큰. 원문({@code token})은 이 응답에서 한 번만 보인다.
 *
 * @param token 토큰 원문
 * @param info 목록에 보이는 정보
 */
public record IssuedToken(String token, PersonalAccessTokenView info) {}
