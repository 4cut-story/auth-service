package net.java21.fourcut.auth.user;

/**
 * 공급자에서 받은 로그인 정보.
 *
 * @param email 대표 이메일. 없을 수 있다 (이메일 없이도 가입할 수 있다)
 * @param emailVerified 공급자가 확인한 이메일인지. 확인된 이메일끼리만 계정을 자동으로 잇는다
 * @param nicknameHint 처음 가입할 때 닉네임으로 쓸 이름 (공급자 로그인 이름 등)
 */
public record LoginProfile(
    Provider provider,
    String providerUserId,
    String email,
    boolean emailVerified,
    String nicknameHint,
    String avatarUrl) {}
