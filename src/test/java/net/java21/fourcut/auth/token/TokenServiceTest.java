package net.java21.fourcut.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.java21.fourcut.auth.AuthProperties;
import net.java21.fourcut.auth.error.ApiException;
import net.java21.fourcut.auth.user.AccountService;
import net.java21.fourcut.auth.user.User;
import net.java21.fourcut.auth.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TokenServiceTest {

  private static final Instant T0 = Instant.parse("2026-11-08T12:00:00Z");

  private final AccessTokens accessTokens = mock(AccessTokens.class);
  private final RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
  private final AccountService accounts = mock(AccountService.class);
  private final UserRepository users = mock(UserRepository.class);
  private final User user = mock(User.class);
  private final UUID family = UUID.randomUUID();
  private RefreshToken token;

  private TokenService at(Instant now) {
    return new TokenService(
        accessTokens,
        refreshTokens,
        accounts,
        users,
        new AuthProperties(
            URI.create("https://x"),
            Duration.ofMinutes(15),
            Duration.ofDays(14),
            Duration.ofMinutes(2),
            null,
            null,
            false,
            URI.create("http://c"),
            URI.create("http://d"),
            Duration.ofSeconds(30)),
        Clock.fixed(now, ZoneOffset.UTC));
  }

  @BeforeEach
  void setUp() {
    token = new RefreshToken(7L, TokenService.hash("raw"), family, T0.plus(Duration.ofDays(14)));
    when(refreshTokens.findByTokenHash(TokenService.hash("raw"))).thenReturn(Optional.of(token));
    when(users.findById(7L)).thenReturn(Optional.of(user));
    when(user.getId()).thenReturn(7L);
    when(user.getPublicId()).thenReturn("u7");
    when(accounts.roleCodes(7L)).thenReturn(List.of());
    when(accessTokens.issue(anyString(), anyList())).thenReturn("access");
  }

  @Test
  @DisplayName("한 페이지의 요청들이 같은 Refresh Token으로 30초 안에 잇달아 갱신하면 모두 새 토큰을 받고 로그인이 끊기지 않는다")
  void concurrentRefreshWithinGrace() {
    TokenPair first = at(T0).refresh("raw");
    TokenPair second = at(T0.plusSeconds(5)).refresh("raw");

    assertThat(first.refreshToken()).isNotEqualTo(second.refreshToken());
    assertThat(token.getUsedAt()).isEqualTo(T0);
    verify(refreshTokens, never()).revokeFamily(any(), any());
  }

  @Test
  @DisplayName("쓴 지 30초가 지난 토큰이 다시 오면 탈취로 보고 그 로그인의 토큰을 모두 폐기한다")
  void reuseAfterGrace() {
    at(T0).refresh("raw");

    assertThatThrownBy(() -> at(T0.plusSeconds(31)).refresh("raw"))
        .isInstanceOf(ApiException.class);
    verify(refreshTokens).revokeFamily(family, T0.plusSeconds(31));
  }

  @Test
  @DisplayName("폐기된 토큰은 유예 시간 안이어도 쓸 수 없다")
  void revokedWithinGrace() {
    at(T0).refresh("raw");
    org.springframework.test.util.ReflectionTestUtils.setField(
        token, "revokedAt", T0.plusSeconds(1));

    assertThatThrownBy(() -> at(T0.plusSeconds(2)).refresh("raw")).isInstanceOf(ApiException.class);
  }
}
