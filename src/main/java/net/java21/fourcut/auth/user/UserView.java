package net.java21.fourcut.auth.user;

import com.fasterxml.jackson.annotation.JsonInclude;

/** 작성자 표시용 사용자 정보. id는 공개 ID다. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserView(String id, String nickname, String avatarUrl) {

  public static UserView of(User user) {
    return new UserView(user.getPublicId(), user.getNickname(), user.getAvatarUrl());
  }
}
