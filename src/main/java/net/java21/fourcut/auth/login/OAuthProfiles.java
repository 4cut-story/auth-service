package net.java21.fourcut.auth.login;

import java.util.Map;
import java.util.Optional;
import net.java21.fourcut.auth.user.LoginProfile;
import net.java21.fourcut.auth.user.Provider;
import org.springframework.stereotype.Component;

/** 공급자 사용자 정보를 {@link LoginProfile}로 바꾼다. */
@Component
public class OAuthProfiles {

  private final GitHubEmails gitHubEmails;

  public OAuthProfiles(GitHubEmails gitHubEmails) {
    this.gitHubEmails = gitHubEmails;
  }

  public LoginProfile from(Provider provider, Map<String, Object> attributes, String accessToken) {
    return switch (provider) {
      case GITHUB -> github(attributes, accessToken);
      case GOOGLE -> google(attributes);
    };
  }

  private LoginProfile github(Map<String, Object> a, String accessToken) {
    Optional<String> verified = gitHubEmails.primaryVerified(accessToken);
    return new LoginProfile(
        Provider.GITHUB,
        String.valueOf(a.get("id")),
        verified.orElse(null),
        verified.isPresent(),
        text(a, "login"),
        text(a, "avatar_url"));
  }

  private static LoginProfile google(Map<String, Object> a) {
    return new LoginProfile(
        Provider.GOOGLE,
        text(a, "sub"),
        text(a, "email"),
        Boolean.TRUE.equals(a.get("email_verified")),
        Optional.ofNullable(text(a, "name")).orElse(text(a, "given_name")),
        text(a, "picture"));
  }

  private static String text(Map<String, Object> a, String key) {
    Object value = a.get(key);
    return value == null ? null : value.toString();
  }
}
