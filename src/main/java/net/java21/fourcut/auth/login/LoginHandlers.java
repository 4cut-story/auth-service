package net.java21.fourcut.auth.login;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import net.java21.fourcut.auth.AuthProperties;
import net.java21.fourcut.auth.error.ApiException;
import net.java21.fourcut.auth.token.AuthCodeStore;
import net.java21.fourcut.auth.user.AccountService;
import net.java21.fourcut.auth.user.Provider;
import net.java21.fourcut.auth.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * 공급자 로그인이 끝나면 회원을 찾거나 만들고, web의 {@code /auth/complete?code=}로 일회용 코드를 넘긴다. 실패하면 web 로그인 화면으로
 * 돌려보낸다. 로그인 과정에만 쓴 세션은 여기서 끝낸다.
 */
@Component
public class LoginHandlers implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

  private static final Logger log = LoggerFactory.getLogger(LoginHandlers.class);

  private final AccountService accounts;
  private final OAuthProfiles profiles;
  private final AuthCodeStore codes;
  private final OAuth2AuthorizedClientService authorizedClients;
  private final AuthProperties properties;

  public LoginHandlers(
      AccountService accounts,
      OAuthProfiles profiles,
      AuthCodeStore codes,
      OAuth2AuthorizedClientService authorizedClients,
      AuthProperties properties) {
    this.accounts = accounts;
    this.profiles = profiles;
    this.codes = codes;
    this.authorizedClients = authorizedClients;
    this.properties = properties;
  }

  @Override
  @SuppressFBWarnings(
      value = "UNVALIDATED_REDIRECT",
      justification = "이동 주소는 설정값 PUBLIC_BASE_URL에 고정 경로만 붙인다")
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {
    OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
    String registrationId = token.getAuthorizedClientRegistrationId();
    String target;
    try {
      Provider provider = Provider.fromRegistrationId(registrationId).orElseThrow();
      OAuth2AuthorizedClient client =
          authorizedClients.loadAuthorizedClient(registrationId, token.getName());
      String accessToken = client == null ? null : client.getAccessToken().getTokenValue();
      User user =
          accounts.login(
              profiles.from(provider, token.getPrincipal().getAttributes(), accessToken));
      target = properties.publicUrl("/auth/complete?code=" + codes.issue(user.getId()));
      if (client != null) {
        authorizedClients.removeAuthorizedClient(registrationId, token.getName());
      }
    } catch (ApiException e) {
      target = loginError(e.code().name());
    }
    endSession(request);
    response.sendRedirect(target);
  }

  @Override
  @SuppressFBWarnings(
      value = "UNVALIDATED_REDIRECT",
      justification = "이동 주소는 설정값 PUBLIC_BASE_URL에 고정 경로만 붙인다")
  public void onAuthenticationFailure(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException {
    log.info("로그인 실패: {}", exception.getMessage());
    endSession(request);
    response.sendRedirect(loginError("LOGIN_FAILED"));
  }

  private String loginError(String code) {
    return properties.publicUrl("/login?error=" + URLEncoder.encode(code, StandardCharsets.UTF_8));
  }

  private static void endSession(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }
  }
}
