package net.java21.fourcut.auth.token;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import net.java21.fourcut.auth.error.ApiException;
import net.java21.fourcut.auth.error.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 토큰 API. web 서버만 부른다 (브라우저는 토큰에 접근하지 않는다). JWKS는 게이트웨이가 클러스터 안에서 읽는다. */
@RestController
public class TokenController {

  private final AuthCodeStore codes;
  private final TokenService tokens;
  private final JwtKeys keys;

  public TokenController(AuthCodeStore codes, TokenService tokens, JwtKeys keys) {
    this.codes = codes;
    this.tokens = tokens;
    this.keys = keys;
  }

  /** 일회용 로그인 코드를 토큰 쌍으로 바꾼다. */
  @PostMapping("/auth/token")
  public TokenPair token(@Valid @RequestBody CodeRequest request) {
    return codes
        .consume(request.code())
        .map(tokens::issue)
        .orElseThrow(() -> new ApiException(ErrorCode.INVALID_AUTH_CODE));
  }

  @PostMapping("/auth/refresh")
  public TokenPair refresh(@Valid @RequestBody RefreshRequest request) {
    return tokens.refresh(request.refreshToken());
  }

  @PostMapping("/auth/logout")
  public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
    tokens.logout(request.refreshToken());
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/.well-known/jwks.json")
  public Map<String, Object> jwks() {
    return keys.publicJwks();
  }

  /** 일회용 로그인 코드. */
  public record CodeRequest(@NotBlank String code) {}

  /** Refresh Token. */
  public record RefreshRequest(@NotBlank String refreshToken) {}
}
