package net.java21.fourcut.auth.login;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/**
 * 설정된 공급자만 로그인 요청으로 바꾼다. 설정되지 않은 공급자는 null을 돌려 {@link LoginController}가 404로 답하게 한다 (기본 구현은 500을
 * 낸다).
 */
public class ConfiguredProviderResolver implements OAuth2AuthorizationRequestResolver {

  private final OAuth2AuthorizationRequestResolver delegate;

  public ConfiguredProviderResolver(OAuthClientRegistrations registrations, String baseUri) {
    this.delegate = new DefaultOAuth2AuthorizationRequestResolver(registrations, baseUri);
  }

  @Override
  public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
    try {
      return delegate.resolve(request);
    } catch (IllegalArgumentException unknownProvider) {
      return null;
    }
  }

  @Override
  public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String registrationId) {
    try {
      return delegate.resolve(request, registrationId);
    } catch (IllegalArgumentException unknownProvider) {
      return null;
    }
  }
}
