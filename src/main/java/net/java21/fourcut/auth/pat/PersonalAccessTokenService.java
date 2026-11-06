package net.java21.fourcut.auth.pat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import net.java21.fourcut.auth.error.ApiException;
import net.java21.fourcut.auth.error.ErrorCode;
import net.java21.fourcut.auth.pat.PersonalAccessTokenEvent.Type;
import net.java21.fourcut.auth.support.PublicId;
import net.java21.fourcut.auth.token.AccessTokens;
import net.java21.fourcut.auth.user.AccountService;
import net.java21.fourcut.auth.user.Role;
import net.java21.fourcut.auth.user.User;
import net.java21.fourcut.auth.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개인 액세스 토큰 발급·조회·폐기와 Access Token 교환.
 *
 * <p>토큰은 web 서버에만 보낸다. web이 이 토큰을 짧은 Access Token으로 바꿔 게이트웨이를 부르고, 게시에 필요한 경로만 열어 준다. 교환한 Access
 * Token에는 {@code scope=publish} 클레임이 붙고 관리자 권한은 빠진다.
 */
@Service
public class PersonalAccessTokenService {

  /** 원문 접두어. 비밀 스캐너와 사람이 알아보기 쉽게 붙인다. */
  public static final String PREFIX = "fcp_";

  static final int MAX_ACTIVE = 5;
  static final int MAX_DAYS = 90;
  static final int DEFAULT_DAYS = 30;
  private static final int HINT_LENGTH = 8;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final PersonalAccessTokenRepository tokens;
  private final PersonalAccessTokenEventRepository events;
  private final UserRepository users;
  private final AccountService accounts;
  private final AccessTokens accessTokens;
  private final Clock clock;

  public PersonalAccessTokenService(
      PersonalAccessTokenRepository tokens,
      PersonalAccessTokenEventRepository events,
      UserRepository users,
      AccountService accounts,
      AccessTokens accessTokens,
      Clock clock) {
    this.tokens = tokens;
    this.events = events;
    this.users = users;
    this.accounts = accounts;
    this.accessTokens = accessTokens;
    this.clock = clock;
  }

  @Transactional
  public IssuedToken issue(String userPublicId, String name, Integer expiresInDays) {
    User user = active(userPublicId);
    int days = expiresInDays == null ? DEFAULT_DAYS : expiresInDays;
    if (days < 1 || days > MAX_DAYS) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "만료일은 1~90일 사이로 정해 주세요.");
    }
    String label = name == null ? "" : name.strip();
    if (label.isEmpty() || label.length() > 50) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "이름은 1~50자로 적어 주세요.");
    }
    Instant now = clock.instant();
    long active =
        tokens.findByUserIdOrderByIdDesc(user.getId()).stream()
            .filter(t -> t.activeAt(now))
            .count();
    if (active >= MAX_ACTIVE) {
      throw new ApiException(ErrorCode.CONFLICT, "토큰은 5개까지 쓸 수 있어요. 안 쓰는 토큰을 먼저 폐기해 주세요.");
    }
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    String raw = PREFIX + secret;
    PersonalAccessToken token =
        tokens.save(
            new PersonalAccessToken(
                user.getId(),
                label,
                hash(raw),
                PREFIX + secret.substring(0, HINT_LENGTH - PREFIX.length()),
                PersonalAccessToken.Scope.PUBLISH,
                now.plus(Duration.ofDays(days))));
    tokens.flush();
    events.save(new PersonalAccessTokenEvent(token.getId(), Type.ISSUED, now));
    return new IssuedToken(raw, PersonalAccessTokenView.of(token));
  }

  @Transactional(readOnly = true)
  public List<PersonalAccessTokenView> list(String userPublicId) {
    User user = active(userPublicId);
    return tokens.findByUserIdOrderByIdDesc(user.getId()).stream()
        .map(PersonalAccessTokenView::of)
        .toList();
  }

  /** 폐기. 이미 폐기한 토큰이어도 성공(멱등). 남의 토큰은 없는 토큰처럼 404. */
  @Transactional
  public void revoke(String userPublicId, String tokenId) {
    User user = active(userPublicId);
    if (!PublicId.isValid(tokenId)) {
      throw ApiException.notFound();
    }
    PersonalAccessToken token =
        tokens.findByPublicIdAndUserId(tokenId, user.getId()).orElseThrow(ApiException::notFound);
    if (token.getRevokedAt() == null) {
      Instant now = clock.instant();
      token.revoke(now);
      events.save(new PersonalAccessTokenEvent(token.getId(), Type.REVOKED, now));
    }
  }

  /** 토큰 원문을 짧은 Access Token으로 바꾼다. 없거나 만료·폐기된 토큰, 탈퇴 처리 중인 회원은 401. */
  @Transactional(noRollbackFor = ApiException.class)
  public ExchangedToken exchange(String raw) {
    if (raw == null || !raw.startsWith(PREFIX)) {
      throw new ApiException(ErrorCode.INVALID_ACCESS_TOKEN);
    }
    Instant now = clock.instant();
    PersonalAccessToken token =
        tokens
            .findByTokenHash(hash(raw))
            .filter(t -> t.activeAt(now))
            .orElseThrow(() -> new ApiException(ErrorCode.INVALID_ACCESS_TOKEN));
    User user =
        users
            .findById(token.getUserId())
            .filter(u -> !u.isDeleting())
            .orElseThrow(() -> new ApiException(ErrorCode.INVALID_ACCESS_TOKEN));
    token.markUsed(now);
    events.save(new PersonalAccessTokenEvent(token.getId(), Type.USED, now));
    List<String> roles =
        accounts.roleCodes(user.getId()).stream().filter(Role.MEMBER::equals).toList();
    String scope = token.getScope().name().toLowerCase(java.util.Locale.ROOT);
    String access = accessTokens.issue(user.getPublicId(), roles, scope, token.getPublicId());
    return new ExchangedToken(access, accessTokens.expiresInSeconds(), scope);
  }

  private User active(String userPublicId) {
    if (!PublicId.isValid(userPublicId)) {
      throw new ApiException(ErrorCode.UNAUTHORIZED);
    }
    return users
        .findByPublicId(userPublicId)
        .filter(u -> !u.isDeleting())
        .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
  }

  static String hash(String raw) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * 교환 결과.
   *
   * @param accessToken Access Token (JWT)
   * @param expiresIn 유효 시간(초)
   * @param scope 권한 범위
   */
  public record ExchangedToken(String accessToken, long expiresIn, String scope) {}
}
