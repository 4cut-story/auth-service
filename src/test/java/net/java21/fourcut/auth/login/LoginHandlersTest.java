package net.java21.fourcut.auth.login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import net.java21.fourcut.auth.AuthProperties;
import net.java21.fourcut.auth.error.ApiException;
import net.java21.fourcut.auth.error.ErrorCode;
import net.java21.fourcut.auth.token.AuthCodeStore;
import net.java21.fourcut.auth.user.AccountService;
import net.java21.fourcut.auth.user.LoginProfile;
import net.java21.fourcut.auth.user.Provider;
import net.java21.fourcut.auth.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

class LoginHandlersTest {

  private final AccountService accounts = mock(AccountService.class);
  private final OAuthProfiles profiles = mock(OAuthProfiles.class);
  private final AuthCodeStore codes = mock(AuthCodeStore.class);
  private final OAuth2AuthorizedClientService clients = mock(OAuth2AuthorizedClientService.class);
  private final AuthProperties properties =
      new AuthProperties(
          URI.create("https://4cut.example.net/"),
          Duration.ofMinutes(15),
          Duration.ofDays(14),
          Duration.ofMinutes(2),
          null,
          null,
          false,
          URI.create("http://c"),
          URI.create("http://d"));
  private final LoginHandlers handlers =
      new LoginHandlers(accounts, profiles, codes, clients, properties);

  private static OAuth2AuthenticationToken token() {
    DefaultOAuth2User principal =
        new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("OAUTH2_USER")), Map.of("id", 1), "id");
    return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "github");
  }

  private static OAuth2AuthorizedClient authorized() {
    ClientRegistration registration =
        ClientRegistration.withRegistrationId("github")
            .clientId("c")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("https://x/cb")
            .authorizationUri("https://a")
            .tokenUri("https://t")
            .build();
    OAuth2AccessToken access =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "tok",
            Instant.now(),
            Instant.now().plusSeconds(60));
    return new OAuth2AuthorizedClient(registration, "1", access);
  }

  @Test
  @DisplayName("로그인에 성공하면 세션을 끝내고 web /auth/complete로 일회용 코드를 넘긴다")
  void success() throws Exception {
    User user = new User("alice", null, null);
    Long id = 42L;
    ReflectionTestUtils.setField(user, "id", id);
    LoginProfile profile = new LoginProfile(Provider.GITHUB, "1", null, false, "alice", null);
    when(clients.loadAuthorizedClient("github", "1")).thenReturn(authorized());
    when(profiles.from(eq(Provider.GITHUB), any(), eq("tok"))).thenReturn(profile);
    when(accounts.login(profile)).thenReturn(user);
    when(codes.issue(id)).thenReturn("CODE");

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.getSession(true);
    MockHttpServletResponse response = new MockHttpServletResponse();
    handlers.onAuthenticationSuccess(request, response, token());

    assertThat(response.getRedirectedUrl())
        .isEqualTo("https://4cut.example.net/auth/complete?code=CODE");
    assertThat(request.getSession(false)).isNull();
  }

  @Test
  @DisplayName("탈퇴 처리 중이면 로그인 화면으로 오류 코드와 함께 돌려보낸다")
  void deleting() throws Exception {
    when(profiles.from(any(), any(), any()))
        .thenReturn(new LoginProfile(Provider.GITHUB, "1", null, false, "a", null));
    when(accounts.login(any())).thenThrow(new ApiException(ErrorCode.ACCOUNT_DELETING));
    MockHttpServletResponse response = new MockHttpServletResponse();
    handlers.onAuthenticationSuccess(new MockHttpServletRequest(), response, token());
    assertThat(response.getRedirectedUrl())
        .isEqualTo("https://4cut.example.net/login?error=ACCOUNT_DELETING");
  }

  @Test
  @DisplayName("공급자 로그인이 실패하면 로그인 화면으로 돌려보낸다")
  void failure() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    handlers.onAuthenticationFailure(
        new MockHttpServletRequest(), response, new BadCredentialsException("x"));
    assertThat(response.getRedirectedUrl())
        .isEqualTo("https://4cut.example.net/login?error=LOGIN_FAILED");
  }
}
