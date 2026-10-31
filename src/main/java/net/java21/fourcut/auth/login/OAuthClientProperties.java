package net.java21.fourcut.auth.login;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 로그인 공급자 OAuth 클라이언트. 값은 Secret({@code auth-service-properties})의 GITHUB_CLIENT_ID 등에서 온다. ID가 비어
 * 있는 공급자는 꺼진다.
 */
@ConfigurationProperties("auth.oauth")
public record OAuthClientProperties(Client github, Client google) {

  public OAuthClientProperties {
    github = github == null ? new Client(null, null) : github;
    google = google == null ? new Client(null, null) : google;
  }

  /** 공급자 하나의 클라이언트 ID와 비밀 값. */
  public record Client(String clientId, String clientSecret) {

    public boolean configured() {
      return clientId != null
          && !clientId.isBlank()
          && clientSecret != null
          && !clientSecret.isBlank();
    }
  }
}
