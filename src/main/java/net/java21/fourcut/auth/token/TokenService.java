package net.java21.fourcut.auth.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import net.java21.fourcut.auth.AuthProperties;
import net.java21.fourcut.auth.error.ApiException;
import net.java21.fourcut.auth.error.ErrorCode;
import net.java21.fourcut.auth.user.AccountService;
import net.java21.fourcut.auth.user.User;
import net.java21.fourcut.auth.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 토큰 발급·갱신·폐기. Refresh Token은 쓸 때마다 바꾼다(rotation). 이미 쓴 토큰이 다시 오면 탈취로 보고 그 로그인의 토큰을 모두 폐기한다. */
@Service
public class TokenService {

  private static final SecureRandom RANDOM = new SecureRandom();

  private final AccessTokens accessTokens;
  private final RefreshTokenRepository refreshTokens;
  private final AccountService accounts;
  private final UserRepository users;
  private final AuthProperties properties;
  private final Clock clock;

  public TokenService(
      AccessTokens accessTokens,
      RefreshTokenRepository refreshTokens,
      AccountService accounts,
      UserRepository users,
      AuthProperties properties,
      Clock clock) {
    this.accessTokens = accessTokens;
    this.refreshTokens = refreshTokens;
    this.accounts = accounts;
    this.users = users;
    this.properties = properties;
    this.clock = clock;
  }

  /** 로그인 직후 새 토큰 쌍 (새 family). */
  @Transactional
  public TokenPair issue(Long userId) {
    User user =
        users.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.INVALID_AUTH_CODE));
    return pair(user, UUID.randomUUID());
  }

  @Transactional(noRollbackFor = ApiException.class, propagation = Propagation.REQUIRED)
  public TokenPair refresh(String rawRefreshToken) {
    Instant now = clock.instant();
    RefreshToken token =
        refreshTokens
            .findByTokenHash(hash(rawRefreshToken))
            .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REFRESH_TOKEN));
    if (token.getUsedAt() != null && token.getRevokedAt() == null) {
      // 이미 쓴 토큰의 재사용: 탈취 가능성이 있으므로 그 로그인의 토큰을 모두 폐기한다
      refreshTokens.revokeFamily(token.getFamilyId(), now);
      throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
    }
    if (!token.usableAt(now)) {
      throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
    }
    User user =
        users
            .findById(token.getUserId())
            .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REFRESH_TOKEN));
    if (user.isDeleting()) {
      throw new ApiException(ErrorCode.ACCOUNT_DELETING);
    }
    token.markUsed(now);
    return pair(user, token.getFamilyId());
  }

  /** 로그아웃: 그 로그인의 Refresh Token을 모두 폐기한다. 없는 토큰이어도 성공(멱등). */
  @Transactional
  public void logout(String rawRefreshToken) {
    refreshTokens
        .findByTokenHash(hash(rawRefreshToken))
        .ifPresent(t -> refreshTokens.revokeFamily(t.getFamilyId(), clock.instant()));
  }

  @Transactional
  public void revokeAll(Long userId) {
    refreshTokens.revokeUser(userId, clock.instant());
  }

  private TokenPair pair(User user, UUID familyId) {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    refreshTokens.save(
        new RefreshToken(
            user.getId(), hash(raw), familyId, clock.instant().plus(properties.refreshTokenTtl())));
    String access = accessTokens.issue(user.getPublicId(), accounts.roleCodes(user.getId()));
    return new TokenPair(access, accessTokens.expiresInSeconds(), raw);
  }

  static String hash(String raw) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
