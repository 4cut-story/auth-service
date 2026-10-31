package net.java21.fourcut.auth.user;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import net.java21.fourcut.auth.error.ApiException;
import net.java21.fourcut.auth.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 로그인 때 회원을 찾거나 만들고, 확인된 같은 이메일이면 두 공급자 계정을 하나로 잇는다 (M-01, M-02). */
@Service
public class AccountService {

  private static final int NICKNAME_MAX = 30;
  private static final Pattern NOT_ALLOWED = Pattern.compile("[^0-9A-Za-z가-힣._-]");
  private static final SecureRandom RANDOM = new SecureRandom();

  private final UserRepository users;
  private final SocialAccountRepository accounts;
  private final RoleRepository roles;
  private final UserRoleRepository userRoles;

  public AccountService(
      UserRepository users,
      SocialAccountRepository accounts,
      RoleRepository roles,
      UserRoleRepository userRoles) {
    this.users = users;
    this.accounts = accounts;
    this.roles = roles;
    this.userRoles = userRoles;
  }

  /** 로그인한 공급자 계정의 회원. 탈퇴 처리 중이면 {@code ACCOUNT_DELETING}. */
  @Transactional
  public User login(LoginProfile profile) {
    User user =
        accounts
            .findByProviderAndProviderUserId(profile.provider(), profile.providerUserId())
            .map(account -> existing(account, profile))
            .orElseGet(() -> linkOrCreate(profile));
    if (user.isDeleting()) {
      throw new ApiException(ErrorCode.ACCOUNT_DELETING);
    }
    user.fillProfile(profile.avatarUrl(), profile.emailVerified() ? profile.email() : null);
    return user;
  }

  @Transactional(readOnly = true)
  public List<String> roleCodes(UUID userId) {
    return roles.findCodesByUserId(userId);
  }

  private User existing(SocialAccount account, LoginProfile profile) {
    account.updateEmail(profile.email(), profile.emailVerified());
    return users.findById(account.getUserId()).orElseThrow(ApiException::notFound);
  }

  private User linkOrCreate(LoginProfile profile) {
    User user = linkTarget(profile).orElseGet(() -> create(profile));
    accounts.save(
        new SocialAccount(
            user.getId(),
            profile.provider(),
            profile.providerUserId(),
            profile.email(),
            profile.emailVerified()));
    return user;
  }

  /** 두 계정 모두 확인된 같은 이메일일 때만 자동으로 잇는다. */
  private Optional<User> linkTarget(LoginProfile profile) {
    if (!profile.emailVerified() || profile.email() == null || profile.email().isBlank()) {
      return Optional.empty();
    }
    return accounts.findVerifiedByEmailOnOtherProvider(profile.email(), profile.provider()).stream()
        .findFirst()
        .flatMap(a -> users.findById(a.getUserId()));
  }

  private User create(LoginProfile profile) {
    User user =
        users.save(
            new User(
                uniqueNickname(profile.nicknameHint()),
                profile.avatarUrl(),
                profile.emailVerified() ? profile.email() : null));
    Role member =
        roles
            .findByCode(Role.MEMBER)
            .orElseThrow(() -> new IllegalStateException("ROLE_MEMBER 기본 데이터가 없음"));
    userRoles.save(new UserRole(user.getId(), member.getId()));
    return user;
  }

  String uniqueNickname(String hint) {
    String base = hint == null ? "" : NOT_ALLOWED.matcher(hint.strip()).replaceAll("");
    if (base.isEmpty()) {
      base = "user";
    }
    if (base.length() > NICKNAME_MAX - 5) {
      base = base.substring(0, NICKNAME_MAX - 5);
    }
    String candidate = base;
    while (users.existsByNickname(candidate)) {
      byte[] suffix = new byte[2];
      RANDOM.nextBytes(suffix);
      candidate = base + "-" + HexFormat.of().formatHex(suffix);
    }
    return candidate;
  }
}
