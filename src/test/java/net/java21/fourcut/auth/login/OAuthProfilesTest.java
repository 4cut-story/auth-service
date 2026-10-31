package net.java21.fourcut.auth.login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import net.java21.fourcut.auth.user.LoginProfile;
import net.java21.fourcut.auth.user.Provider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OAuthProfilesTest {

  private final GitHubEmails emails = mock(GitHubEmails.class);
  private final OAuthProfiles profiles = new OAuthProfiles(emails);

  @Test
  @DisplayName("GitHub은 이메일 목록 API의 확인된 대표 이메일만 쓴다")
  void github() {
    when(emails.primaryVerified("tok")).thenReturn(Optional.of("a@x.com"));
    LoginProfile p =
        profiles.from(
            Provider.GITHUB, Map.of("id", 42, "login", "alice", "avatar_url", "u"), "tok");
    assertThat(p).isEqualTo(new LoginProfile(Provider.GITHUB, "42", "a@x.com", true, "alice", "u"));

    when(emails.primaryVerified("none")).thenReturn(Optional.empty());
    LoginProfile none = profiles.from(Provider.GITHUB, Map.of("id", 7), "none");
    assertThat(none.email()).isNull();
    assertThat(none.emailVerified()).isFalse();
  }

  @Test
  @DisplayName("Google은 sub, email, email_verified, name(없으면 given_name)을 쓴다")
  void google() {
    LoginProfile p =
        profiles.from(
            Provider.GOOGLE,
            Map.of("sub", "s1", "email", "b@x.com", "email_verified", true, "given_name", "Bo"),
            null);
    assertThat(p).isEqualTo(new LoginProfile(Provider.GOOGLE, "s1", "b@x.com", true, "Bo", null));
    assertThat(Provider.fromRegistrationId("kakao")).isEmpty();
  }
}
