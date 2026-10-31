package net.java21.fourcut.auth.login;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import net.java21.fourcut.auth.AuthProperties;
import net.java21.fourcut.auth.error.ApiException;
import net.java21.fourcut.auth.error.ErrorCode;
import net.java21.fourcut.auth.token.AuthCodeStore;
import net.java21.fourcut.auth.user.AccountService;
import net.java21.fourcut.auth.user.LoginProfile;
import net.java21.fourcut.auth.user.Provider;
import net.java21.fourcut.auth.user.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인 보조 API. 설정된 공급자의 {@code /auth/login/{provider}}는 Spring Security가 먼저 처리하고(공급자로 302), 여기에는
 * 설정되지 않은 공급자만 온다.
 */
@RestController
public class LoginController {

  private final OAuthClientRegistrations registrations;
  private final AccountService accounts;
  private final AuthCodeStore codes;
  private final AuthProperties properties;

  public LoginController(
      OAuthClientRegistrations registrations,
      AccountService accounts,
      AuthCodeStore codes,
      AuthProperties properties) {
    this.registrations = registrations;
    this.accounts = accounts;
    this.codes = codes;
    this.properties = properties;
  }

  /** 켜진 로그인 공급자 (web 로그인 화면이 버튼을 고를 때 쓴다). */
  @GetMapping("/auth/providers")
  public Providers providers() {
    return new Providers(registrations.enabled(), properties.devLoginEnabled());
  }

  @GetMapping("/auth/login/{provider}")
  public void notConfigured(@PathVariable String provider) {
    throw new ApiException(ErrorCode.NOT_FOUND, "이 로그인 방식은 아직 설정되지 않았어요.");
  }

  /** 개발용 로그인. {@code AUTH_DEV_LOGIN_ENABLED=true}일 때만 쓴다 (local). 운영에서는 404. */
  @PostMapping("/auth/dev-login")
  public CodeResponse devLogin(@Valid @RequestBody DevLoginRequest request) {
    if (!properties.devLoginEnabled()) {
      throw ApiException.notFound();
    }
    User user =
        accounts.login(
            new LoginProfile(
                Provider.GITHUB,
                "dev:" + request.nickname(),
                null,
                false,
                request.nickname(),
                null));
    return new CodeResponse(codes.issue(user.getId()));
  }

  /** 켜진 공급자 목록. */
  public record Providers(List<String> items, boolean devLogin) {}

  /** 개발용 로그인 요청. */
  public record DevLoginRequest(@NotBlank @Size(max = 20) String nickname) {}

  /** 일회용 로그인 코드. */
  public record CodeResponse(String code) {}
}
