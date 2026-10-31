package net.java21.fourcut.auth.user;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.java21.fourcut.auth.error.ApiException;
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

  public MeView me(UUID userId) {
    User user = active(userId);
    List<Provider> providers =
        accounts.findByUserId(userId).stream()
            .map(SocialAccount::getProvider)
            .distinct()
            .sorted(Comparator.naturalOrder())
            .toList();
    return new MeView(user.getId(), user.getNickname(), user.getAvatarUrl(), providers);
  }

  public UserView user(UUID userId) {
    return UserView.of(active(userId));
  }

  public List<UserView> users(List<UUID> ids) {
    return users.findByIdInAndStatus(ids, User.Status.ACTIVE).stream().map(UserView::of).toList();
  }

  private User active(UUID userId) {
    return users.findById(userId).filter(u -> !u.isDeleting()).orElseThrow(ApiException::notFound);
  }
}
