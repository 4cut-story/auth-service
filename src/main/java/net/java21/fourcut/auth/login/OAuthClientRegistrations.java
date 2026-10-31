package net.java21.fourcut.auth.login;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.java21.fourcut.auth.AuthProperties;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;

/**
 * 설정된 공급자만 등록한다. redirect_uri는 공개 주소 기준 {@code {PUBLIC_BASE_URL}/auth/callback/{github|google}}이다
 * (브라우저 → web → 게이트웨이 → auth-service로 그대로 전달된다).
 */
@Component
public class OAuthClientRegistrations implements ClientRegistrationRepository {

  private final Map<String, ClientRegistration> registrations;

  public OAuthClientRegistrations(OAuthClientProperties clients, AuthProperties properties) {
    Map<String, ClientRegistration> map = new LinkedHashMap<>();
    if (clients.github().configured()) {
      map.put(
          "github",
          CommonOAuth2Provider.GITHUB
              .getBuilder("github")
              .clientId(clients.github().clientId())
              .clientSecret(clients.github().clientSecret())
              .redirectUri(properties.publicUrl("/auth/callback/github"))
              .scope("read:user", "user:email")
              .build());
    }
    if (clients.google().configured()) {
      map.put(
          "google",
          CommonOAuth2Provider.GOOGLE
              .getBuilder("google")
              .clientId(clients.google().clientId())
              .clientSecret(clients.google().clientSecret())
              .redirectUri(properties.publicUrl("/auth/callback/google"))
              .scope("openid", "profile", "email")
              .build());
    }
    this.registrations = Collections.unmodifiableMap(map);
  }

  @Override
  public ClientRegistration findByRegistrationId(String registrationId) {
    return registrations.get(registrationId);
  }

  /** 켜진 공급자의 registrationId 목록. web이 로그인 버튼을 고를 때 쓴다. */
  public List<String> enabled() {
    return new ArrayList<>(registrations.keySet());
  }
}
