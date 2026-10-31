package net.java21.fourcut.auth.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import net.java21.fourcut.auth.TestcontainersConfiguration;
import net.java21.fourcut.auth.error.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, AccountService.class})
class AccountServiceTest {

  @Autowired private AccountService service;
  @Autowired private SocialAccountRepository accounts;
  @Autowired private UserRepository users;

  private static LoginProfile github(String id, String email, boolean verified, String login) {
    return new LoginProfile(Provider.GITHUB, id, email, verified, login, "https://avatars/" + id);
  }

  private static LoginProfile google(String id, String email, boolean verified, String name) {
    return new LoginProfile(Provider.GOOGLE, id, email, verified, name, null);
  }

  @Test
  @DisplayName("M-01: 처음 로그인하면 회원과 ROLE_MEMBER를 만들고, 다시 로그인하면 같은 회원이다")
  void createsOnceAndReturnsSameUser() {
    User first = service.login(github("1", "a@x.com", true, "alice"));
    User again = service.login(github("1", "a@x.com", true, "alice"));

    assertThat(again.getId()).isEqualTo(first.getId());
    assertThat(first.getNickname()).isEqualTo("alice");
    assertThat(first.getAvatarUrl()).isEqualTo("https://avatars/1");
    assertThat(service.roleCodes(first.getId())).containsExactly("ROLE_MEMBER");
  }

  @Test
  @DisplayName("M-02: 두 계정 모두 확인된 같은 이메일이면 하나의 회원으로 잇는다 (대소문자 무시)")
  void linksVerifiedSameEmail() {
    User gh = service.login(github("2", "Bob@x.com", true, "bob"));
    User gg = service.login(google("g2", "bob@X.com", true, "Bob Kim"));

    assertThat(gg.getId()).isEqualTo(gh.getId());
    assertThat(accounts.findByUserId(gh.getId())).hasSize(2);
  }

  @Test
  @DisplayName("M-02: 한쪽 이메일이 확인되지 않았으면 잇지 않고 새 회원을 만든다")
  void doesNotLinkUnverified() {
    User gh = service.login(github("3", "carol@x.com", true, "carol"));
    User gg = service.login(google("g3", "carol@x.com", false, "carol"));
    assertThat(gg.getId()).isNotEqualTo(gh.getId());

    User noEmail = service.login(github("4", null, false, null));
    assertThat(noEmail.getNickname()).isEqualTo("user");
    assertThat(noEmail.getEmail()).isNull();
  }

  @Test
  @DisplayName("닉네임이 겹치면 뒤에 짧은 무작위 값을 붙이고, 쓸 수 없는 글자는 뺀다")
  void uniqueNickname() {
    service.login(github("5", null, false, "dave"));
    User other = service.login(google("g5", null, false, "dave"));
    assertThat(other.getNickname()).startsWith("dave-").hasSize(9);
    assertThat(service.uniqueNickname("홍 길동!!")).isEqualTo("홍길동");
    assertThat(service.uniqueNickname("x".repeat(40))).hasSize(25);
  }

  @Test
  @DisplayName("탈퇴 처리 중인 계정은 로그인할 수 없다 (ACCOUNT_DELETING)")
  void deletingCannotLogin() {
    User user = service.login(github("6", null, false, "erin"));
    user.markDeleting();
    users.saveAndFlush(user);
    assertThatThrownBy(() -> service.login(github("6", null, false, "erin")))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("탈퇴");
  }
}
