package net.java21.fourcut.auth.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * 작성자 표시용 사용자 정보. id는 공개 ID, nickname은 주소(/@아이디)에 쓰는 로그인 아이디, displayName은 사용자가 정한 표시 이름(없으면
 * 빠진다)이다. joinedAt은 가입 시각이다 (프로필).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserView(
    String id, String nickname, String displayName, String avatarUrl, Instant joinedAt) {

  public static UserView of(User user) {
    return new UserView(
        user.getPublicId(),
        user.getNickname(),
        user.getDisplayName(),
        user.getAvatarUrl(),
        user.getCreatedAt());
  }
}
