package net.java21.fourcut.auth.user;

import net.java21.fourcut.auth.error.ApiException;
import net.java21.fourcut.auth.error.ErrorCode;
import net.java21.fourcut.auth.support.PublicId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 내 프로필 수정. 표시 이름은 겹쳐도 된다 (주소에 쓰는 로그인 아이디만 고유하다). */
@Service
public class ProfileService {

  private final UserRepository users;
  private final UserQueryService queries;

  public ProfileService(UserRepository users, UserQueryService queries) {
    this.users = users;
    this.queries = queries;
  }

  @Transactional
  public MeView changeDisplayName(String userId, String displayName) {
    if (!PublicId.isValid(userId)) {
      throw ApiException.notFound();
    }
    User user =
        users
            .findByPublicId(userId)
            .filter(u -> !u.isDeleting())
            .orElseThrow(ApiException::notFound);
    user.changeDisplayName(normalize(displayName));
    users.flush();
    return queries.me(userId);
  }

  /** 앞뒤 공백을 지운다. 비면 null, 제어 문자가 있거나 너무 길면 400. */
  static String normalize(String displayName) {
    if (displayName == null) {
      return null;
    }
    String value = displayName.strip();
    if (value.isEmpty()) {
      return null;
    }
    if (value.codePointCount(0, value.length()) > User.MAX_DISPLAY_NAME
        || value.codePoints().anyMatch(Character::isISOControl)) {
      throw new ApiException(
          ErrorCode.VALIDATION_FAILED, "닉네임은 " + User.MAX_DISPLAY_NAME + "자 이하로 적어 주세요.");
    }
    return value;
  }
}
