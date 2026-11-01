package net.java21.fourcut.auth.user;

import java.util.Comparator;
import java.util.List;
import net.java21.fourcut.auth.error.ApiException;
import net.java21.fourcut.auth.support.PublicId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 프로필 조회. 탈퇴 처리 중인 회원은 없는 회원처럼 다룬다. */
@Service
@Transactional(readOnly = true)
public class UserQueryService {

  private final UserRepository users;
  private final SocialAccountRepository accounts;

  public UserQueryService(UserRepository users, SocialAccountRepository accounts) {
    this.users = users;
    this.accounts = accounts;
  }

  public MeView me(String userId) {
    User user = active(userId);
    List<Provider> providers =
        accounts.findByUserId(user.getId()).stream()
            .map(SocialAccount::getProvider)
            .distinct()
            .sorted(Comparator.naturalOrder())
            .toList();
    return new MeView(user.getPublicId(), user.getNickname(), user.getAvatarUrl(), providers);
  }

  public UserView user(String userId) {
    return UserView.of(active(userId));
  }

  /** 닉네임은 회원마다 고유하다. 없거나 탈퇴 처리 중이면 404. */
  public UserView byNickname(String nickname) {
    return users
        .findByNickname(nickname == null ? "" : nickname.strip())
        .filter(u -> !u.isDeleting())
        .map(UserView::of)
        .orElseThrow(ApiException::notFound);
  }

  public List<UserView> users(List<String> ids) {
    return users.findByPublicIdInAndStatus(ids, User.Status.ACTIVE).stream()
        .map(UserView::of)
        .toList();
  }

  /** 공개 ID로 찾는다. 형식이 틀린 ID도 없는 회원처럼 404로 답한다. */
  private User active(String userId) {
    if (!PublicId.isValid(userId)) {
      throw ApiException.notFound();
    }
    return users
        .findByPublicId(userId)
        .filter(u -> !u.isDeleting())
        .orElseThrow(ApiException::notFound);
  }
}
