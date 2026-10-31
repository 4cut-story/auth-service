package net.java21.fourcut.auth.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

/** 작성자 표시용 사용자 정보. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserView(UUID id, String nickname, String avatarUrl) {

  public static UserView of(User user) {
    return new UserView(user.getId(), user.getNickname(), user.getAvatarUrl());
  }
}
