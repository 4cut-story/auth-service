package net.java21.fourcut.auth.pat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import net.java21.fourcut.auth.user.ItemsResponse;
import net.java21.fourcut.auth.web.HeaderNames;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * 개인 액세스 토큰. 발급·목록·폐기는 로그인한 회원이 쓴다(게이트웨이가 붙인 {@code X-User-Id}). web은 이 경로를 개인 토큰으로는 열지 않는다(토큰으로
 * 토큰을 만들 수 없다). 교환({@code /auth/pat/exchange})은 web 서버만 부른다.
 */
@RestController
public class PersonalAccessTokenController {

  private final PersonalAccessTokenService service;

  public PersonalAccessTokenController(PersonalAccessTokenService service) {
    this.service = service;
  }

  @PostMapping("/api/v1/me/tokens")
  public ResponseEntity<IssuedToken> issue(
      @RequestHeader(HeaderNames.USER_ID) String userId, @Valid @RequestBody IssueRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.issue(userId, request.name(), request.expiresInDays(), request.scope()));
  }

  @GetMapping("/api/v1/me/tokens")
  public ItemsResponse<PersonalAccessTokenView> list(
      @RequestHeader(HeaderNames.USER_ID) String userId) {
    List<PersonalAccessTokenView> items = service.list(userId);
    return new ItemsResponse<>(items);
  }

  @DeleteMapping("/api/v1/me/tokens/{id}")
  public ResponseEntity<Void> revoke(
      @RequestHeader(HeaderNames.USER_ID) String userId, @PathVariable String id) {
    service.revoke(userId, id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/auth/pat/exchange")
  public PersonalAccessTokenService.ExchangedToken exchange(
      @Valid @RequestBody ExchangeRequest request) {
    return service.exchange(request.token());
  }

  /** 발급 요청. 만료일을 비우면 30일, 권한 범위를 비우면 publish. */
  public record IssueRequest(@NotBlank String name, Integer expiresInDays, String scope) {}

  /** 교환 요청. */
  public record ExchangeRequest(@NotBlank String token) {}
}
